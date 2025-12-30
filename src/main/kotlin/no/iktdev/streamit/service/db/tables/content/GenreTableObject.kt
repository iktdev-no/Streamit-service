package no.iktdev.streamit.service.db.tables.content

import org.jetbrains.exposed.sql.ResultRow

data class GenreTableObject(
    val id: Int,
    val genre: String
) {
    companion object {
        fun fromRow(row: ResultRow): GenreTableObject {
            return GenreTableObject(
                id = row[GenreTable.id].value,
                genre = row[GenreTable.genre]
            )
        }
    }
}