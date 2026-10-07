package no.iktdev.streamit.service.db.tables.content.v2

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
object SubtitleTableV2 : LongIdTable(name = "SUBTITLE") {
    val videoId: Column<EntityID<Long>> = reference("VIDEO_ID", VideoTableV2)
    val language: Column<String> = varchar("LANGUAGE", 20)
    val format: Column<String> = varchar("FORMAT", 20)
    val file: Column<String> = varchar("FILE", 500)

    init {
        uniqueIndex(videoId, language, format)
    }
}