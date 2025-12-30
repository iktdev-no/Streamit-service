package no.iktdev.streamit.service.api.meta

import no.iktdev.streamit.service.db.tables.info.DataAudioTable
import no.iktdev.streamit.service.db.tables.info.DataVideoTable
import no.iktdev.streamit.service.dto.AudioData
import no.iktdev.streamit.service.dto.StreamData
import no.iktdev.streamit.service.dto.VideoData
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

/**
 *
 * CURRENTLY NOT IN USE!
 *
 * **/

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
            val data = DataAudioTable.selectAll().where { DataAudioTable.file eq file }.singleOrNull()
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
            val data = DataVideoTable.selectAll().where { DataVideoTable.file eq file }.singleOrNull()
            videodata = data?.let { VideoData.Companion.fromRow(it) }
        }
        return videodata
    }

}