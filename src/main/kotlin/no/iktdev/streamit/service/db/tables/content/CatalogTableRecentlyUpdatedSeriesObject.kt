package no.iktdev.streamit.service.db.tables.content

import java.time.LocalDateTime

data class CatalogTableRecentlyUpdatedSeriesObject(
    val id: Int,
    val title: String,
    val cover: String?,
    val type: String,
    val collection: String,
    val iid: Int?,
    val genres: String?,
    val added: LocalDateTime,
    val serieTableEntryAdded: LocalDateTime
)

