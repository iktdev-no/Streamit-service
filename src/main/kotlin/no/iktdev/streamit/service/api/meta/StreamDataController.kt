package no.iktdev.streamit.service.api.meta

import no.iktdev.streamit.library.db.tables.other.DataAudioTable
import no.iktdev.streamit.library.db.tables.other.DataVideoTable
import no.iktdev.streamit.shared.classes.AudioData
import no.iktdev.streamit.shared.classes.StreamData
import no.iktdev.streamit.shared.classes.VideoData
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

/**
 *
 * CURRENTLY NOT IN USE!
 *
 * **/

@RestController
class StreamDataController {

    @GetMapping("/data/stream/{file}")
    fun getStreamData(@PathVariable file: String?): StreamData? {
        return StreamData(getVideoStream(file), getAudioStream(file))
    }


    @GetMapping("/data/stream/audio/{file}")
    fun getAudioStream(@PathVariable file: String? = null): AudioData?
    {
        var audioData: AudioData? = null
        if (file.isNullOrEmpty()) return null
        transaction {
            val data = DataAudioTable.select { DataAudioTable.file eq file }.singleOrNull()
            audioData = data?.let { AudioData.Companion.fromRow(it) }
        }
        return audioData
    }

    @GetMapping("/data/stream/video/{file}")
    fun getVideoStream(@PathVariable file: String? = null): VideoData?
    {
        var videodata: VideoData? = null
        if (file.isNullOrEmpty()) return null
        transaction {
            val data = DataVideoTable.select { DataVideoTable.file eq file }.singleOrNull()
            videodata = data?.let { VideoData.Companion.fromRow(it) }
        }
        return videodata
    }

}