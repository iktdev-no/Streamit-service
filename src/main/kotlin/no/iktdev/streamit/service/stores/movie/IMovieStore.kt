package no.iktdev.streamit.service.stores.movie

interface IMovieStore {
    fun insert(catalogId: Long, videoId: Long): Boolean
}