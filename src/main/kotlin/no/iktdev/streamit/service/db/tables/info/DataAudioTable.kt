package no.iktdev.streamit.service.db.tables.info

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object DataAudioTable : IntIdTable(name = "MEDIA_DATA_AUDIO") {
    val file: Column<String> = varchar("SOURCE", 200).uniqueIndex() // Currently Audio Stream is embedded in video file. Might change at a later date
    val codec: Column<String> = varchar("CODEC", 12)
    val channels: Column<Int?> = integer("CHANNELS").nullable()
    val sample_rate: Column<Int?> = integer("SAMPLE_RATE").nullable()
    val layout: Column<String?> = varchar("LAYOUT", 8).nullable()
    val language: Column<String> = varchar("LANGUAGE", 6)
}