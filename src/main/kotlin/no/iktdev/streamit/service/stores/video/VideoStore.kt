package no.iktdev.streamit.service.stores.video

import mu.KotlinLogging
import no.iktdev.streamit.service.db.tables.content.v2.VideoTableV2
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.model.shared.consumer.content.Video
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component

@Component
class VideoStore : IVideoStore {
    val log = KotlinLogging.logger {}

    override fun getById(id: Long): Video? = withTransaction {
        VideoTableV2
            .selectAll()
            .where { VideoTableV2.id eq id }
            .singleOrNull()
            ?.let {
                Video(
                    id = it[VideoTableV2.id].value,
                    videoFileName = it[VideoTableV2.file],
                    duration = it[VideoTableV2.duration] ?: 0L
                )
            }
    }.getOrNull()

    override fun getByFile(file: String): Video? = withTransaction {
        VideoTableV2
            .selectAll()
            .where { VideoTableV2.file eq file }
            .orderBy(VideoTableV2.id to SortOrder.ASC)
            .firstOrNull()
            ?.let {
                Video(
                    id = it[VideoTableV2.id].value,
                    videoFileName = it[VideoTableV2.file],
                    duration = it[VideoTableV2.duration] ?: 0L
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

    override fun update(videoFile: String, existing: Video): Video = withTransaction {
        if (existing.videoFileName != videoFile) {
            val updated = VideoTableV2.update(where = { VideoTableV2.id eq existing.id }) {
                it[VideoTableV2.file] = videoFile
            }
            check(updated > 0) { "Video ${existing.id} no longer exists" }
            log.info("Overwriting video file for videoId={} from '{}' to '{}'", existing.id, existing.videoFileName, videoFile)
        }
        Video(
            id = existing.id,
            videoFileName = videoFile,
            duration = existing.duration
        )
    }.getOrThrow()

}
