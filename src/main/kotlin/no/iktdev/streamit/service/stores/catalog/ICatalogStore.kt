package no.iktdev.streamit.service.stores.catalog

import no.iktdev.streamit.service.model.shared.content.Catalog
import no.iktdev.streamit.service.model.shared.content.ContentType
import no.iktdev.streamit.service.model.shared.content.Movie
import no.iktdev.streamit.service.model.shared.content.Serie
import java.util.UUID

interface ICatalogStore {

    fun insert(
        store: String,
        type: ContentType,
        cover: String? = null
    ): Long

    fun updateCover(id: Long, cover: String): Boolean

    fun getAll(offset: Long = 0, limit: Int? = 100): List<Catalog>
    fun getByIds(ids: List<Long>): List<Catalog>

    fun getMovies(offset: Long = 0, limit: Int = 100): List<Catalog>
    fun getSeries(offset: Long = 0, limit: Int = 100): List<Catalog>
    fun getMovieById(id: Long): Movie?
    fun getSerieById(id: Long): Serie?
    fun getSerieByStore(store: String): Serie?

    fun getByGenre(
        genreId: Long,
        offset: Long,
        limit: Int
    ): List<Catalog>

    fun getRecentlyAdded(freshness: Long): List<Catalog>
    fun getRecentlyUpdatedSeries(): List<Catalog>

    fun getContinueOrResumeSerie(userId: UUID): List<Serie>

    fun search(keyword: String): List<Catalog>
    fun searchMovies(keyword: String): List<Catalog>
    fun searchSeries(keyword: String): List<Catalog>
    fun getMovieByIdWithProgress(id: Long, userId: UUID): Movie?
    fun getSerieByIdWithProgress(id: Long, userId: UUID): Serie?
}