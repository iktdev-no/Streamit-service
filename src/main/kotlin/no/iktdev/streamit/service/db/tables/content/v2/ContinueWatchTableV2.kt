package no.iktdev.streamit.service.db.tables.content.v2

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object ContinueWatchTableV2 : LongIdTable(name = "CONTINUE_WATCH") {
    val userId: Column<EntityID<Long>> = reference("USER_ID", UserTableV2, onDelete = ReferenceOption.CASCADE)
    val catalogId: Column<EntityID<Long>> = reference("CATALOG_ID", CatalogTableV2, onDelete = ReferenceOption.CASCADE)
    val videoId: Column<EntityID<Long>> = reference("VIDEO_ID", VideoTableV2, onDelete = ReferenceOption.CASCADE)
    val lastPlayed: Column<LocalDateTime> = datetime("LAST_PLAYED")
    val hide: Column<Boolean> = bool("HIDE").default(false)

    init {
        uniqueIndex(userId, catalogId)
        index("idx_continue_watch_user_played", false, userId, lastPlayed)
    }
}
