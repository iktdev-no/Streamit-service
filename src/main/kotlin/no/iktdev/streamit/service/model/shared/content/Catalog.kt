package no.iktdev.streamit.service.model.shared.content

enum class ContentType {
    Movie,
    Serie,
}


open class Catalog(
    val id: Long,
    val title: String,
    val cover: String?,
    val type: ContentType,
    val collection: String,
    var genres: List<Genre> = emptyList(),
    val recent: Boolean
) {

}

class Movie(
    id: Long, // id will be catalog id
    title: String,
    cover: String? = null,
    collection: String,
    genres: List<Genre> = emptyList(),
    recent: Boolean = false,
    val video: Video,
    var subtitles: List<Subtitle> = emptyList(),
    val progress: Progress? = null,
) : Catalog(
    id = id,
    title = title,
    cover = cover,
    type = ContentType.Movie,
    collection = collection,
    genres = genres,
    recent = recent
) {
}


class Serie(
    id: Long,
    title: String,
    cover: String? = null,
    collection: String,
    genres: List<Genre> = emptyList(),
    recent: Boolean = false,
    var episodes: List<Episode> = emptyList()
) : Catalog(
    id = id,
    title = title,
    cover = cover,
    type = ContentType.Serie,
    collection = collection,
    genres = genres,
    recent = recent
) {

    fun after(currentSeason: Int, currentEpisode: Int): Episode? {
        return episodes.filter { s -> s.season >= currentSeason }.firstOrNull { e -> e.episode >= currentEpisode }
    }
}
