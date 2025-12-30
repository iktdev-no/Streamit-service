package no.iktdev.streamit.service.db.queries

import no.iktdev.streamit.service.dto.Catalog
import no.iktdev.streamit.service.Env
import no.iktdev.streamit.service.db.tables.content.CatalogTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

fun CatalogTable.executeGetAll(): List<Catalog> =  transaction {
    CatalogTable.selectAll()
        .mapNotNull { Catalog.fromRow(it) }
}

fun CatalogTable.executeGetByIds(ids: List<Int>): List<Catalog> = transaction {
    CatalogTable.selectAll().where { CatalogTable.id inList ids }
        .mapNotNull { Catalog.fromRow(it) }
}

fun CatalogTable.executeGetMovies(): List<Catalog> =
    CatalogTable.selectOnlyMovies()
        .map { Catalog.Companion.fromTable(it) }

fun CatalogTable.executeGetSeries(): List<Catalog> =
    CatalogTable.selectOnlySeries()
        .map { Catalog.Companion.fromTable(it) }

fun CatalogTable.executeFindWithGenres(): List<Catalog> =
    CatalogTable.selectWhereGenreIsSet()
        .map { Catalog.Companion.fromTable(it) }


fun CatalogTable.executeGetRecentlyAdded(): List<Catalog> =
    CatalogTable.selectRecentlyAdded(Env.frshness.toInt())
        .map { Catalog.Companion.fromTable(it) }

fun CatalogTable.executeGetRecentlyUpdatedSeries(): List<Catalog> {
    val recentAdded = Env.getSerieCutoff()
    val dateTime = LocalDateTime.now().minusDays(Env.frshness)

   return CatalogTable.selectNewlyUpdatedSeries(noOlderThan = recentAdded)
        .map {
            val recent = it.serieTableEntryAdded > dateTime
            Catalog.Companion.fromTable(it, recent)
        }
}

fun CatalogTable.executeSearchWith(keyword: String) =
    CatalogTable.searchWith(keyword)
        .map { Catalog.Companion.fromTable(it) }

fun CatalogTable.executeSearchForMovie(keyword: String) =
    CatalogTable.searchMovieWith(keyword)
        .map { Catalog.Companion.fromTable(it) }


fun CatalogTable.executeSearchForSerie(keyword: String) =
    CatalogTable.searchSerieWith(keyword)
        .map { Catalog.Companion.fromTable(it) }