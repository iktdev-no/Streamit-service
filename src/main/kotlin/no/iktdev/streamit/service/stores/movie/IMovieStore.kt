package no.iktdev.streamit.service.stores.movie

import no.iktdev.streamit.service.model.shared.content.Movie

interface IMovieStore {
    fun insert(catalogId: Long, videoId: Long): Boolean
}