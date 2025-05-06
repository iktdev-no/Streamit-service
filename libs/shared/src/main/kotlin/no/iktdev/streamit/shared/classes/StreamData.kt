package no.iktdev.streamit.shared.classes

import no.iktdev.streamit.library.db.tables.other.DataAudioTable
import no.iktdev.streamit.library.db.tables.other.DataVideoTable
import org.jetbrains.exposed.sql.ResultRow

data class StreamData(
    val video: VideoData?,
    val audio: AudioData?
)


data class VideoData(
    val codec: String,
    val pixelFormat: String,
    val colorSpace: String?
) {
    companion object
    {
        fun fromRow(row: ResultRow) = VideoData(
            codec =  row[DataVideoTable.codec],
            pixelFormat = row[DataVideoTable.pixelFormat],
            colorSpace = row[DataVideoTable.colorSpace]
        )
    }
}

data class AudioData(
    val codec: String,
    val layout: String?,
    val language: String
) {
    companion object
    {
        fun fromRow(row: ResultRow) = AudioData(
            codec =  row[DataAudioTable.codec],
            layout = row[DataAudioTable.layout],
            language = row[DataAudioTable.language]
        )
    }
}