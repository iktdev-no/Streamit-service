package no.iktdev.streamit.service.db.tables.content.v2

import no.iktdev.streamit.service.db.tables.user.UserTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object ContinueWatchTableV2 : LongIdTable(name = "CONTINUE_WATCH") {
    val userId: Column<EntityID<Long>> = reference("USER_ID", UserTableV2)
    val catalogId: Column<EntityID<Long>> = reference("CATALOG_ID", CatalogTableV2)
    val hide: Column<Boolean> = bool("HIDE").default(false)

    init {
        uniqueIndex(userId, catalogId)
    }
}