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
            val rows = ContinueWatchTableV2
                .selectAll()
                .where {
                    (ContinueWatchTableV2.userId eq user.id) and
                        (ContinueWatchTableV2.hide eq false)
                }
                .orderBy(ContinueWatchTableV2.lastPlayed to SortOrder.DESC)
                .limit(maxItems)
                .toList()

            if (rows.isEmpty()) return@withTransaction emptyList()

            val catalogIds = rows.map { it[ContinueWatchTableV2.catalogId].value }.distinct()
            val videoIds = rows.map { it[ContinueWatchTableV2.videoId].value }.distinct()
            val catalogs = CatalogTableV2.selectAll()
                .where { CatalogTableV2.id inList catalogIds.map { id -> EntityID(id, CatalogTableV2) } }
                .associateBy { it[CatalogTableV2.id].value }
            val videos = VideoTableV2.selectAll()
                .where { VideoTableV2.id inList videoIds.map { id -> EntityID(id, VideoTableV2) } }
                .associateBy { it[VideoTableV2.id].value }
            val titles = CatalogTitleTableV2.selectAll()
                .where {
                    (CatalogTitleTableV2.catalogId inList catalogIds.map { id -> EntityID(id, CatalogTableV2) }) and
                        (CatalogTitleTableV2.preferred eq true)
                }
                .orderBy(CatalogTitleTableV2.id to SortOrder.ASC)
                .toList()
                .distinctBy { it[CatalogTitleTableV2.catalogId].value }
                .associate { it[CatalogTitleTableV2.catalogId].value to it[CatalogTitleTableV2.title] }
            val series = SerieTableV2.selectAll()
                .where { SerieTableV2.videoId inList videoIds.map { id -> EntityID(id, VideoTableV2) } }
                .associateBy { it[SerieTableV2.videoId].value }
            val progressByVideo = ProgressTableV2.selectAll()
                .where {
                    (ProgressTableV2.userId eq user.id) and
                        (ProgressTableV2.videoId inList videoIds.map { id -> EntityID(id, VideoTableV2) })
                }
                .associate { row ->
                    row[ProgressTableV2.videoId].value to Progress(
                        position = row[ProgressTableV2.position],
                        played = row[ProgressTableV2.played].toProgressInstant()
                    )
                }

            rows.mapNotNull { row ->
                val catalogId = row[ContinueWatchTableV2.catalogId].value
                val videoId = row[ContinueWatchTableV2.videoId].value
                val catalog = catalogs[catalogId] ?: return@mapNotNull null
                val title = titles[catalogId] ?: return@mapNotNull null
                val videoRow = videos[videoId] ?: return@mapNotNull null
                val seriesRow = series[videoId]

                ContinueToWatch(
                    catalogId = catalogId,
                    title = title,
                    cover = catalog[CatalogTableV2.cover],
                    type = catalog[CatalogTableV2.type],
                    video = Video(
                        id = videoId,
                        videoFileName = videoRow[VideoTableV2.file],
                        duration = videoRow[VideoTableV2.duration] ?: 0L
                    ),
                    progress = progressByVideo[videoId],
                    episode = seriesRow?.let {
                        ContinueWatchingEpisode(
                            season = it[SerieTableV2.season],
                            number = it[SerieTableV2.episode],
                            title = it[SerieTableV2.title]
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
        val video = VideoTableV2.selectAll()
            .where { VideoTableV2.id eq videoId }
            .singleOrNull()
            ?: return
        val duration = video[VideoTableV2.duration]
        val isFinished = duration != null && duration > 0 &&
            duration - progress.position <= FINISHED_REMAINING_MILLIS
        val played = progress.played.toProgressDateTime()

        val movie = MovieTableV2.selectAll()
            .where { MovieTableV2.videoId eq videoId }
            .singleOrNull()
        if (movie != null) {
            val catalogId = movie[MovieTableV2.catalogId].value
            if (isStaleUpdate(userId, catalogId, played)) return
            if (isFinished) {
                deleteEntry(userId, catalogId)
            } else {
                upsertEntry(userId, catalogId, videoId, played)
            }
            return
        }

        val episode = SerieTableV2.selectAll()
            .where { SerieTableV2.videoId eq videoId }
            .singleOrNull()
            ?: return
        val catalogId = episode[SerieTableV2.catalogId].value
        if (isStaleUpdate(userId, catalogId, played)) return

        if (!isFinished) {
            upsertEntry(userId, catalogId, videoId, played)
            return
        }

        val season = episode[SerieTableV2.season]
        val number = episode[SerieTableV2.episode]
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

    private fun isStaleUpdate(userId: Long, catalogId: Long, played: java.time.LocalDateTime): Boolean =
        ContinueWatchTableV2.selectAll()
            .where {
                (ContinueWatchTableV2.userId eq userId) and
                    (ContinueWatchTableV2.catalogId eq catalogId)
            }
            .singleOrNull()
            ?.get(ContinueWatchTableV2.lastPlayed)
            ?.isAfter(played) == true

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
