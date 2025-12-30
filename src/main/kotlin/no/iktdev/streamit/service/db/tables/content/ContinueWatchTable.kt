package no.iktdev.streamit.service.db.tables.content

import no.iktdev.streamit.service.db.tables.util.UpsertResult
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.update

object ContinueWatchTable: IntIdTable(name = "CONTINUE_WATCH") {
    val userId: Column<String> = char("USER_ID", 36)
    val type: Column<String> = varchar("TYPE", 10)
    val title: Column<String> = varchar("TITLE", 250)
    val collection: Column<String?> = varchar("COLLECTION", 250).nullable()
    val hide: Column<Boolean> = bool("HIDE").default(false)


    fun updateVisibility(userId: String, type: String, collection: String, hide: Boolean = false): UpsertResult {
        val update = ContinueWatchTable.update({
            this@ContinueWatchTable.collection.eq(collection)
                .and(ContinueWatchTable.type.eq(type))
                .and(ContinueWatchTable.userId.eq(userId))
                .and(ContinueWatchTable.collection.eq(collection))
        }) {
            it[this.hide] = hide
        }
        return if (update > 0) UpsertResult.Updated else UpsertResult.Skipped
    }
}