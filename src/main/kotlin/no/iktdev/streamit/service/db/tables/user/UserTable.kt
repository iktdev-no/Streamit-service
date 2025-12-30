package no.iktdev.streamit.service.db.tables.user

import no.iktdev.streamit.service.db.tables.util.UpsertResult
import no.iktdev.streamit.service.dto.User
import org.jetbrains.exposed.sql.*

object UserTable : Table(name = "USERS") {
    val guid: Column<String> = varchar("USER_ID", 50)
    val name: Column<String> = varchar("NAME", 50).uniqueIndex()
    val image: Column<String> = varchar("IMAGE", 200)

    fun selectUser(userId: String): Query {
        return UserTable.selectAll().where { guid eq userId }
    }

    fun upsert(userId: String, name: String, image: String): UpsertResult {
        val insert = UserTable.insertIgnore {
            it[this.guid] = userId
            it[this.name] = name
            it[this.image] = image
        }

        return if (insert.insertedCount > 0) {
            UpsertResult.Inserted(insert)
        } else {
            val update = UserTable.update({
                (guid eq userId)
            }) {
                it[this.name] = name
                it[this.image] = name
            }
            if (update > 0) UpsertResult.Updated else UpsertResult.Skipped

        }
    }

}