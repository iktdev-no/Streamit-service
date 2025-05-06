package no.iktdev.streamit.shared.database.queries

import no.iktdev.streamit.library.db.tables.content.SubtitleTable
import no.iktdev.streamit.shared.classes.Subtitle
import org.jetbrains.exposed.sql.transactions.transaction

fun SubtitleTable.executeFindForVideo(videoFile: String) = transaction {
    SubtitleTable.findSubtitleForVideo(videoFile)
        .mapNotNull { Subtitle.fromRow(it) }
}

fun SubtitleTable.executeFindForVideo(videoFile: String, format: String) = transaction {
    SubtitleTable.findSubtitleForVideo(videoFile, format)
        .mapNotNull { Subtitle.fromRow(it) }
}