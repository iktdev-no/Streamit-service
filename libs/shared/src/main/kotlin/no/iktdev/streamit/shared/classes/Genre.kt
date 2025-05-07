package no.iktdev.streamit.shared.classes

import no.iktdev.streamit.library.db.tables.content.GenreTable
import org.jetbrains.exposed.sql.ResultRow

data class Genre(val id: Int, val genre: String)
{
    companion object
    {
        fun fromRow(resultRow: ResultRow) = Genre(
            id = resultRow[GenreTable.id].value,
            genre = resultRow[GenreTable.genre]
        )
    }
}

data class GenreCatalog(val genre: Genre, val catalog: MutableList<Catalog> = mutableListOf())