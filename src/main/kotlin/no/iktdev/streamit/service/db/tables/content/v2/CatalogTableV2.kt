package no.iktdev.streamit.service.db.tables.content.v2

import no.iktdev.streamit.service.model.shared.content.ContentType
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object CatalogTableV2 : LongIdTable(name = "CATALOG") {
    val store: Column<String> = varchar("STORE", 500)
    val cover: Column<String?> = varchar("COVER", 500).nullable()
    val type: Column<ContentType> =
        enumerationByName("TYPE", 10, ContentType::class)
    val addedAt: Column<LocalDateTime> = datetime("ADDED_AT")
    val updatedAt: Column<LocalDateTime> = datetime("UPDATED_AT")
}
