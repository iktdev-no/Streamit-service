package no.iktdev.streamit.service.dto

import no.iktdev.streamit.service.db.tables.content.SubtitleTable
import org.jetbrains.exposed.sql.ResultRow

data class Subtitle(val id: Int, val associatedWithVideo: String, val language: String, val subtitle: String, val collection: String?, val format: String)
{
    companion object
    {
        fun fromRow(row: ResultRow) = Subtitle(
            id = row[SubtitleTable.id].value,
            associatedWithVideo = row[SubtitleTable.associatedWithVideo],
            language = row[SubtitleTable.language],
            subtitle = row[SubtitleTable.subtitle], // The subtitle file
            collection = row[SubtitleTable.collection],
            format = row[SubtitleTable.format]
        )
    }
}