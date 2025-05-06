package no.iktdev.streamit.shared.database.queries

import no.iktdev.streamit.library.db.tables.content.CatalogTable
import no.iktdev.streamit.shared.classes.Genre
import no.iktdev.streamit.library.db.tables.content.GenreTable
import no.iktdev.streamit.shared.classes.Catalog
import no.iktdev.streamit.shared.classes.GenreCatalog
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun GenreTable.executeSelectAll(): List<Genre> {
    return transaction {
        this@executeSelectAll.selectAll().mapNotNull { Genre.fromRow(it) }
    }
}

fun GenreTable.executeSelectById(id: Int = -1): Genre? {
    if (id < 0) return null
    val row = transaction {
        this@executeSelectById.select { this@executeSelectById.id.eq(id) }.singleOrNull()
    }
    return if (row == null) null else Genre.fromRow(row)
}

fun GenreTable.executeGetByIds(ids: List<Int>): List<Genre> {
    return if (ids.isNotEmpty()) transaction {
        this@executeGetByIds.select { this@executeGetByIds.id inList ids.toList() }.map { Genre.fromRow(it) }
    } else emptyList()
}

fun GenreTable.executeGetCatalogGroupedByGenre(): List<GenreCatalog>  {
    val genres = GenreTable.executeSelectAll().associate { it.id to GenreCatalog(it) }
    CatalogTable.executeFindWithGenres().forEach { catalog ->
        catalog.genres?.split(",")?.mapNotNull { gid -> gid.toIntOrNull() }?.forEach { genreId ->
            genres[genreId]?.catalog?.add(catalog)
        }
    }
    return genres.values.filter { it.catalog.size > 3 }
}