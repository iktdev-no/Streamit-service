package no.iktdev.streamit.service.db.tables.info

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object DataVideoTable : IntIdTable(name = "MEDIA_DATA_VIDEO") {
    val file: Column<String> = varchar("SOURCE", 200).uniqueIndex()
    val codec: Column<String> = varchar("CODEC", 12)
    val pixelFormat: Column<String> = varchar("PIXEL_FORMAT", 12)
    val colorSpace: Column<String?> = varchar("COLOR_SPACE", 8).nullable()
}