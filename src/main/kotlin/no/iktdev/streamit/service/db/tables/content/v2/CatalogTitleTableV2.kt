package no.iktdev.streamit.service.db.tables.content.v2

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
object CatalogTitleTableV2 : LongIdTable(name = "CATALOG_TITLE") {
    val catalogId: Column<EntityID<Long>> = reference("CATALOG_ID", CatalogTableV2)
    val title: Column<String> = varchar("TITLE", 500)
    val language: Column<String> = varchar("LANGUAGE", 16)
    val preferred: Column<Boolean> = bool("PREFERRED").default(false)

    init {
        uniqueIndex(catalogId, title, language)
    }
}