package no.iktdev.streamit.service.stores.serie

import no.iktdev.streamit.service.model.shared.content.Episode

interface ISerieStore {
    fun insert(
        catalogId: Long,
        videoId: Long,
        season: Int,
        episode: Int,
        title: String?
    ): Boolean
}