package no.iktdev.streamit.service.stores.video

import no.iktdev.streamit.service.db.tables.content.v2.VideoTableV2
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.model.shared.content.Video
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class VideoStore : IVideoStore {

    override fun getByFile(file: String): Video? = withTransaction {
        VideoTableV2
            .selectAll()
            .where { VideoTableV2.file eq file }
            .singleOrNull()
            ?.let {
                Video(
                    id = it[VideoTableV2.id].value,
                    videoFileName = it[VideoTableV2.file],
                    duration = it[VideoTableV2.duration]?.toLong() ?: 0L
                )
            }
    }.getOrNull()
    override fun insert(
        file: String,
        duration: Long?
    ): Video = withTransaction {
        val id = VideoTableV2.insertAndGetId {
            it[VideoTableV2.file] = file
            it[VideoTableV2.duration] = duration
        }.value

        Video(
            id = id,
            videoFileName = file,
            duration = duration ?: 0L
        )
    }.getOrThrow()
}