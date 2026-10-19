package no.iktdev.streamit.service.stores.movie

interface IMovieStore {
    fun getVideoId(catalogId: Long): Long?
    fun insert(catalogId: Long, videoId: Long): Boolean
}
