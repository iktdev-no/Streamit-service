package no.iktdev.streamit.shared.classes

import no.iktdev.streamit.library.db.objects.content.CatalogTableObject
import no.iktdev.streamit.library.db.objects.content.RecentlyUpdatedSeriesHolderObject
import no.iktdev.streamit.library.db.tables.content.CatalogTable
import no.iktdev.streamit.library.db.tables.content.MovieTable
import org.jetbrains.exposed.sql.ResultRow

enum class ContentType {
    Movie,
    Serie,
    Unknown
}

fun ContentType.sqlName(): String {
    return this.name.lowercase()
}

abstract class BaseEpisode {
    abstract val season: Int
    abstract val episode: Int
    abstract val video: String
}


open class Catalog(
    val id: Int,
    val title: String,
    val cover: String?,
    val type: ContentType,
    val collection: String,
    var genres: String?,
    val recent: Boolean // If true on serie, shoud display new episodes, if movie, should display just new
) {
    companion object {
        fun fromRow(resultRow: ResultRow, recent: Boolean = false) = Catalog(
            id = resultRow[CatalogTable.id].value,
            title = resultRow[CatalogTable.title],
            cover = resultRow[CatalogTable.cover],
            type = resultRow[CatalogTable.type].let {
                if (it.equals("movie", true))
                    ContentType.Movie
                else if (it.equals(
                        "serie",
                        true
                    )
                ) ContentType.Serie else ContentType.Unknown
            },
            collection = resultRow[CatalogTable.collection],
            genres = resultRow[CatalogTable.genres] ?: "",
            recent = recent
        )
        fun fromTable(item: CatalogTableObject, recent: Boolean = false) = Catalog(
            id = item.id,
            title = item.title,
            cover = item.cover,
            type = item.type.let {
                if (it.equals("movie", true))
                    ContentType.Movie
                else if (it.equals(
                        "serie",
                        true
                    )
                ) ContentType.Serie else ContentType.Unknown
            },
            collection = item.collection,
            genres = item.genres ?: "",
            recent = recent
        )

        fun fromTable(item: RecentlyUpdatedSeriesHolderObject, recent: Boolean = false) = Catalog(
            id = item.id,
            title = item.title,
            cover = item.cover,
            type = item.type.let {
                if (it.equals("movie", true))
                    ContentType.Movie
                else if (it.equals(
                        "serie",
                        true
                    )
                ) ContentType.Serie else ContentType.Unknown
            },
            collection = item.collection,
            genres = item.genres ?: "",
            recent = recent
        )
    }
}

class Movie(
    id: Int, // id will be catalog id
    title: String,
    cover: String? = null,
    collection: String,
    genres: String? = "",
    recent: Boolean = false,
    val video: String,
    var progress: Int = 0,
    var duration: Int = 0,
    var played: Int = 0,
    var subtitles: List<Subtitle> = emptyList()
) : Catalog(
    id = id,
    title = title,
    cover = cover,
    type = ContentType.Movie,
    collection = collection,
    genres = genres,
    recent = recent
) {
    companion object {
        fun fromRow(resultRow: ResultRow, recent: Boolean = false) = Movie(
            id = resultRow[CatalogTable.id].value,
            video = resultRow[MovieTable.video],
            title = resultRow[CatalogTable.title],
            cover = resultRow[CatalogTable.cover],
            collection = resultRow[CatalogTable.collection],
            genres = resultRow[CatalogTable.genres] ?: "",
            recent = recent
        )

        fun toCatalog(it: Movie) = Catalog(
            id = it.id,
            title = it.title,
            cover = it.cover,
            type = it.type,
            collection = it.collection,
            genres = it.genres,
            recent = it.recent
        )
    }
}

data class Episode(
    override val season: Int,
    override val episode: Int,
    val title: String?,
    override val video: String,
    var progress: Int = 0,
    var duration: Int = 0,
    var played: Int = 0,
    var subtitles: List<Subtitle> = emptyList()
) : BaseEpisode()


class Serie(
    id: Int,
    title: String,
    cover: String? = null,
    collection: String,
    genres: String?,
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
    companion object {
        fun basedOn(row: ResultRow) = Serie(
            id = row[CatalogTable.id].value,
            title = row[CatalogTable.title],
            cover = row[CatalogTable.cover],
            collection = row[CatalogTable.collection],
            genres = row[CatalogTable.genres]?: "",
        )
    }

    fun shallowCopy() = Serie(id, title, cover, collection, genres, recent)


    fun after(currentSeason: Int, currentEpisode: Int): Episode? {
        return episodes.filter { s -> s.season >= currentSeason }.firstOrNull { e -> e.episode >= currentEpisode }
    }
}
