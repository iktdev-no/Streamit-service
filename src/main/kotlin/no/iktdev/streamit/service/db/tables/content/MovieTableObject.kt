package no.iktdev.streamit.service.db.tables.content

import org.jetbrains.exposed.sql.ResultRow

data class MovieTableObject(
    val id: Int,
    val video: String
) {
    companion object {
        fun fromRow(row: ResultRow): MovieTableObject {
            return MovieTableObject(
                id = row[MovieTable.id].value,
                video = row[MovieTable.video]
            )
        }
    }
}