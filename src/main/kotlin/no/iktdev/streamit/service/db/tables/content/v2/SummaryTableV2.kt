package no.iktdev.streamit.service.db.tables.content.v2

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
object SummaryTableV2 : LongIdTable(name = "SUMMARY") {
    val catalogId: Column<EntityID<Long>> = reference("CATALOG_ID", CatalogTableV2)
    val description: Column<String> = text("DESCRIPTION")
    val language: Column<String> = varchar("LANGUAGE", 16)

    init {
        uniqueIndex(catalogId, language)
    }
}