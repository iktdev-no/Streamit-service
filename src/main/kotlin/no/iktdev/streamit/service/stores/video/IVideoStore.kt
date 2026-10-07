package no.iktdev.streamit.service.stores.video

import no.iktdev.streamit.service.model.shared.content.Video

interface IVideoStore {
    fun getByFile(file: String): Video?
    fun insert(file: String, duration: Long? = null): Video
}