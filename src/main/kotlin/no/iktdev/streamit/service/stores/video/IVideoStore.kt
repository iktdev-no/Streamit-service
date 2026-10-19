package no.iktdev.streamit.service.stores.video

import no.iktdev.streamit.service.model.shared.consumer.content.Video

interface IVideoStore {
    fun getByFile(file: String): Video?
    fun getById(id: Long): Video?
    fun insert(file: String, duration: Long? = null): Video
    fun update(videoFile: String, existing: Video): Video
}
