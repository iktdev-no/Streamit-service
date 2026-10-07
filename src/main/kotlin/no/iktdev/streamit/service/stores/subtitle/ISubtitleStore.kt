package no.iktdev.streamit.service.stores.subtitle

import no.iktdev.streamit.service.model.shared.content.Subtitle

interface ISubtitleStore {
    fun insert(videoId: Long, language: String, format: String, file: String): Subtitle
    fun update(id: Long, language: String, format: String, file: String): Subtitle?
    fun delete(id: Long)
}