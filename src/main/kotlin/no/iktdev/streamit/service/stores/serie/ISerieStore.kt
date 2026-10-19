package no.iktdev.streamit.service.stores.serie

interface ISerieStore {
    fun getVideoId(catalogId: Long, season: Int, episode: Int): Long?
    fun insert(
        catalogId: Long,
        videoId: Long,
        season: Int,
        episode: Int,
        title: String?
    ): Boolean
}
