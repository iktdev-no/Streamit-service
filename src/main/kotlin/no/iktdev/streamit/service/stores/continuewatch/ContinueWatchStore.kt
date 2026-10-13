package no.iktdev.streamit.service.stores.continuewatch

import no.iktdev.streamit.service.Env
import no.iktdev.streamit.service.db.tables.content.v2.CatalogTableV2
import no.iktdev.streamit.service.db.tables.content.v2.CatalogTitleTableV2
import no.iktdev.streamit.service.db.tables.content.v2.ContinueWatchTableV2
import no.iktdev.streamit.service.db.tables.content.v2.MovieTableV2
import no.iktdev.streamit.service.db.tables.content.v2.ProgressTableV2
import no.iktdev.streamit.service.db.tables.content.v2.SerieTableV2
import no.iktdev.streamit.service.db.tables.content.v2.UserTableV2
import no.iktdev.streamit.service.db.tables.content.v2.VideoTableV2
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.model.shared.consumer.content.ContinueToWatch
import no.iktdev.streamit.service.model.shared.consumer.content.ContinueWatchingEpisode
import no.iktdev.streamit.service.model.shared.consumer.content.Progress
import no.iktdev.streamit.service.model.shared.consumer.content.Video
import no.iktdev.streamit.service.stores.progress.toProgressDateTime
import no.iktdev.streamit.service.stores.progress.toProgressInstant
import no.iktdev.streamit.service.stores.user.IUserStore
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ContinueWatchStore(
    private val userStore: IUserStore
) : IContinueWatchStore {

    override fun getForUser(userId: UUID): List<ContinueToWatch> {
        val user = userStore.getByUid(userId) ?: return emptyList()
        val maxItems = Env.continueWatch.coerceAtLeast(0)
        if (maxItems == 0) return emptyList()

        return withTransaction {
            val entryIds = ContinueWatchTableV2
                .selectAll()
                .where {
                    (ContinueWatchTableV2.userId eq user.id) and
                        (ContinueWatchTableV2.hide eq false)
                }
                .orderBy(ContinueWatchTableV2.lastPlayed to SortOrder.DESC)
                .limit(maxItems)
                .map { it[ContinueWatchTableV2.id] }

            if (entryIds.isEmpty()) return@withTransaction emptyList()

            val rows = ContinueWatchTableV2
                .innerJoin(CatalogTableV2)
                .innerJoin(CatalogTitleTableV2)
                .innerJoin(VideoTableV2)
                .leftJoin(
                    ProgressTableV2,
                    additionalConstraint = {
                        (ProgressTableV2.videoId eq VideoTableV2.id) and
                            (ProgressTableV2.userId eq EntityID(user.id, UserTableV2))
                    }
                )
                .leftJoin(
                    SerieTableV2,
                    additionalConstraint = { SerieTableV2.videoId eq VideoTableV2.id }
                )
                .selectAll()
                .where {
                    (ContinueWatchTableV2.id inList entryIds) and
                        (CatalogTitleTableV2.preferred eq true)
                }
                .orderBy(
                    ContinueWatchTableV2.lastPlayed to SortOrder.DESC,
                    CatalogTitleTableV2.id to SortOrder.ASC
                )
                .toList()
                // Multiple titles can be preferred; choose the first one by title ID.
                .distinctBy { it[ContinueWatchTableV2.id].value }

            rows.map { row ->
                val catalogId = row[ContinueWatchTableV2.catalogId].value
                val videoId = row[ContinueWatchTableV2.videoId].value

                ContinueToWatch(
                    catalogId = catalogId,
                    title = row[CatalogTitleTableV2.title],
                    cover = row[CatalogTableV2.cover],
                    type = row[CatalogTableV2.type],
                    video = Video(
                        id = videoId,
                        videoFileName = row[VideoTableV2.file],
                        duration = row[VideoTableV2.duration] ?: 0L
                    ),
                    progress = row.getOrNull(ProgressTableV2.position)?.let { position ->
                        Progress(
                            position = position,
                            played = row[ProgressTableV2.played].toProgressInstant()
                        )
                    },
                    episode = row.getOrNull(SerieTableV2.season)?.let { season ->
                        ContinueWatchingEpisode(
                            season = season,
                            number = row[SerieTableV2.episode],
                            title = row[SerieTableV2.title]
                        )
                    }
                )
            }
        }.getOrDefault(emptyList())
    }

    override fun hide(userId: UUID, catalogId: Long): Boolean {
        val user = userStore.getByUid(userId) ?: return false
        return withTransaction {
            ContinueWatchTableV2.update(
                where = {
                    (ContinueWatchTableV2.userId eq user.id) and
                        (ContinueWatchTableV2.catalogId eq catalogId)
                }
            ) {
                it[ContinueWatchTableV2.hide] = true
            } > 0
        }.getOrDefault(false)
    }

    override fun syncFromProgress(userId: Long, videoId: Long, progress: Progress) {
        val state = VideoTableV2
            .leftJoin(
                MovieTableV2,
                additionalConstraint = { MovieTableV2.videoId eq VideoTableV2.id }
            )
            .leftJoin(
                SerieTableV2,
                additionalConstraint = { SerieTableV2.videoId eq VideoTableV2.id }
            )
            .leftJoin(
                ContinueWatchTableV2,
                additionalConstraint = {
                    (ContinueWatchTableV2.userId eq EntityID(userId, UserTableV2)) and
                        ((ContinueWatchTableV2.catalogId eq MovieTableV2.catalogId) or
                            (ContinueWatchTableV2.catalogId eq SerieTableV2.catalogId))
                }
            )
            .selectAll()
            .where { VideoTableV2.id eq videoId }
            .singleOrNull()
            ?: return

        val duration = state[VideoTableV2.duration]
        val isFinished = duration != null && duration > 0 &&
            duration - progress.position <= FINISHED_REMAINING_MILLIS
        val played = progress.played.toProgressDateTime()
        val existingLastPlayed = state.getOrNull(ContinueWatchTableV2.lastPlayed)
        if (existingLastPlayed?.isAfter(played) == true) return

        val movieCatalogId = state.getOrNull(MovieTableV2.catalogId)?.value
        if (movieCatalogId != null) {
            if (isFinished) {
                deleteEntry(userId, movieCatalogId)
            } else {
                upsertEntry(userId, movieCatalogId, videoId, played)
            }
            return
        }

        val catalogId = state.getOrNull(SerieTableV2.catalogId)?.value ?: return

        if (!isFinished) {
            upsertEntry(userId, catalogId, videoId, played)
            return
        }

        val season = state[SerieTableV2.season]
        val number = state[SerieTableV2.episode]
        val nextEpisode = SerieTableV2.selectAll()
            .where {
                (SerieTableV2.catalogId eq catalogId) and
                    ((SerieTableV2.season greater season) or
                        ((SerieTableV2.season eq season) and (SerieTableV2.episode greater number)))
            }
            .orderBy(SerieTableV2.season to SortOrder.ASC, SerieTableV2.episode to SortOrder.ASC)
            .firstOrNull()

        if (nextEpisode == null) {
            deleteEntry(userId, catalogId)
        } else {
            upsertEntry(userId, catalogId, nextEpisode[SerieTableV2.videoId].value, played)
        }
    }

    private fun upsertEntry(userId: Long, catalogId: Long, videoId: Long, played: java.time.LocalDateTime) {
        ContinueWatchTableV2.upsert {
            it[ContinueWatchTableV2.userId] = EntityID(userId, UserTableV2)
            it[ContinueWatchTableV2.catalogId] = EntityID(catalogId, CatalogTableV2)
            it[ContinueWatchTableV2.videoId] = EntityID(videoId, VideoTableV2)
            it[ContinueWatchTableV2.lastPlayed] = played
        }
    }

    private fun deleteEntry(userId: Long, catalogId: Long) {
        ContinueWatchTableV2.deleteWhere {
            (ContinueWatchTableV2.userId eq userId) and
                (ContinueWatchTableV2.catalogId eq catalogId)
        }
    }

    private companion object {
        // Video positions and durations are stored in milliseconds by the Android clients.
        const val FINISHED_REMAINING_MILLIS = 5 * 60 * 1000L
    }
}
