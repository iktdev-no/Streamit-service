package no.iktdev.streamit.service.stores.progress

import no.iktdev.streamit.service.db.tables.content.v2.ProgressTableV2
import no.iktdev.streamit.service.db.tables.content.v2.UserTableV2
import no.iktdev.streamit.service.db.tables.content.v2.VideoTableV2
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.model.shared.content.Progress
import no.iktdev.streamit.service.stores.user.IUserStore
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProgressStore(
    private val userStore: IUserStore
) : IProgressStore {
    override fun get(
        userId: UUID,
        videoId: Long
    ): Progress? {
        val user = userStore.getByUid(userId)
            ?: return null

        return withTransaction {
            ProgressTableV2
                .selectAll()
                .where {
                    (ProgressTableV2.userId eq user.id) and
                            (ProgressTableV2.videoId eq videoId)
                }
                .singleOrNull()
                ?.let {
                    Progress(
                        position = it[ProgressTableV2.position],
                        played = it[ProgressTableV2.played].toProgressInstant()
                    )
                }
        }.getOrNull()
    }

    override fun getAll(userId: UUID): List<Progress> {
        val user = userStore.getByUid(userId)
            ?: return emptyList()

        return withTransaction {
            ProgressTableV2
                .selectAll()
                .where {
                    ProgressTableV2.userId eq user.id
                }
                .map {
                    Progress(
                        position = it[ProgressTableV2.position],
                        played = it[ProgressTableV2.played].toProgressInstant()
                    )
                }
        }.getOrDefault(emptyList())
    }

    override fun getAfter(
        userId: UUID,
        played: java.time.Instant
    ): List<Progress> {
        val user = userStore.getByUid(userId)
            ?: return emptyList()

        return withTransaction {
            ProgressTableV2
                .selectAll()
                .where {
                    (ProgressTableV2.userId eq user.id) and
                    (ProgressTableV2.played greater played.toProgressDateTime())
                }
                .map {
                    Progress(
                        position = it[ProgressTableV2.position],
                        played = it[ProgressTableV2.played].toProgressInstant()
                    )
                }
        }.getOrDefault(emptyList())
    }

    override fun upsert(
        userId: UUID,
        videoId: Long,
        progress: Progress
    ): Boolean {
        val user = userStore.getByUid(userId)
            ?: return false

        return withTransaction {
            // user.id er Long
            ProgressTableV2.upsert {
                it[ProgressTableV2.userId] =
                    EntityID(user.id, UserTableV2)

                it[ProgressTableV2.videoId] =
                    EntityID(videoId, VideoTableV2)

                it[ProgressTableV2.position] =
                    progress.position

                it[ProgressTableV2.played] =
                    progress.played.toProgressDateTime()

            }

            true
        }.isSuccess
    }
}
