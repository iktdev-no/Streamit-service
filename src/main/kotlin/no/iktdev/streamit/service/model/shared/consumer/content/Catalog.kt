package no.iktdev.streamit.service.model.shared.consumer.content

import no.iktdev.streamit.service.model.shared.ContentType
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema as OpenApiSchema


@OpenApiSchema(
    oneOf = [Movie::class, Serie::class],
    discriminatorProperty = "type",
    discriminatorMapping = [
        DiscriminatorMapping(value = "Movie", schema = Movie::class),
        DiscriminatorMapping(value = "Serie", schema = Serie::class)
    ]
)
open class Catalog(
    val id: Long,
    val title: String,
    val cover: String?,
    open val type: ContentType,
    val collection: String,
    var genres: List<Genre> = emptyList(),
    val recent: Boolean
) {

}

@OpenApiSchema(allOf = [Catalog::class])
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
    @get:OpenApiSchema(allowableValues = ["Movie"])
    override val type: ContentType = ContentType.Movie
}


@OpenApiSchema(allOf = [Catalog::class])
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
    @get:OpenApiSchema(allowableValues = ["Serie"])
    override val type: ContentType = ContentType.Serie

    fun after(currentSeason: Int, currentEpisode: Int): Episode? {
        return episodes.filter { s -> s.season >= currentSeason }.firstOrNull { e -> e.episode >= currentEpisode }
    }
}
