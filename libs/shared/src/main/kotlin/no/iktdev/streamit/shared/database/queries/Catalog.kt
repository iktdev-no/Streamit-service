package no.iktdev.streamit.shared.database.queries

import no.iktdev.streamit.shared.classes.Catalog
import no.iktdev.streamit.library.db.tables.content.CatalogTable
import no.iktdev.streamit.library.db.tables.content.SerieTable
import no.iktdev.streamit.shared.Env
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

fun CatalogTable.executeGetAll(): List<Catalog> = transaction {
    CatalogTable.selectAll()
        .mapNotNull { Catalog.fromRow(it) }
}

fun CatalogTable.executeGetMovies(): List<Catalog> = transaction {
    CatalogTable.selectOnlyMovies()
        .mapNotNull { Catalog.fromRow(it) }
}

fun CatalogTable.executeGetSeries(): List<Catalog> = transaction {
    CatalogTable.selectOnlySeries()
        .mapNotNull { Catalog.fromRow(it) }
}

fun CatalogTable.executeFindWithGenres(): List<Catalog> = transaction {
    CatalogTable.selectWhereGenreIsSet()
        .mapNotNull { Catalog.fromRow(it) }
}

fun CatalogTable.executeGetRecentlyAdded(): List<Catalog> = transaction {
    CatalogTable.selectRecentlyAdded(Env.frshness.toInt())
        .mapNotNull { Catalog.fromRow(it) }
}

fun CatalogTable.executeGetRecentlyUpdatedSeries(): List<Catalog> = transaction {
    val recentAdded = Env.getSerieCutoff()
    val dateTime = LocalDateTime.now().minusDays(Env.frshness)

    CatalogTable.selectNewlyUpdatedSeries(noOlderThan = recentAdded)
        .mapNotNull {
            val recent = it[SerieTable.added] > dateTime
            Catalog.fromRow(it, recent)
        }
}

fun CatalogTable.executeSearchWith(keyword: String) = transaction {
    CatalogTable.searchWith(keyword)
        .mapNotNull { Catalog.fromRow(it) }
}

fun CatalogTable.executeSearchForMovie(keyword: String) = transaction {
    CatalogTable.searchMovieWith(keyword)
        .mapNotNull { Catalog.fromRow(it) }
}

fun CatalogTable.executeSearchForSerie(keyword: String) = transaction {
    CatalogTable.searchSerieWith(keyword)
        .mapNotNull { Catalog.fromRow(it) }
}