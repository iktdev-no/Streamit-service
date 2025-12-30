package no.iktdev.streamit.service.db.tables.content

import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDateTime

data class CatalogTableObject(
    val id: Int,
    val title: String,
    val cover: String?,
    val type: String,
    val collection: String,
    val iid: Int?,
    val genres: String?,
    val added: LocalDateTime
) {
    companion object {
        fun fromRow(resultRow: ResultRow) =
            CatalogTableObject(
                id = resultRow[CatalogTable.id].value,
                title = resultRow[CatalogTable.title],
                cover = resultRow[CatalogTable.cover],
                type = resultRow[CatalogTable.type],
                collection = resultRow[CatalogTable.collection],
                iid = resultRow[CatalogTable.iid],
                genres = resultRow[CatalogTable.genres],
                added = resultRow[CatalogTable.added]
            )

    }
}

