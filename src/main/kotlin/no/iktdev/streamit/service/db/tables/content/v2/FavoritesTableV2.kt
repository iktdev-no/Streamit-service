package no.iktdev.streamit.service.db.tables.content.v2

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
object FavoritesTableV2 : LongIdTable(name = "FAVORITES") {
    val userId: Column<EntityID<Long>> = reference("USER_ID", UserTableV2)
    val catalogId: Column<EntityID<Long>> = reference("CATALOG_ID", CatalogTableV2)

    init {
        uniqueIndex(userId, catalogId)
    }
}