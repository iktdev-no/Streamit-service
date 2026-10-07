package no.iktdev.streamit.service.db.tables.content.v2

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object UserTableV2 : LongIdTable(name = "USERS") {
    val uid: Column<String> = varchar("USER_ID", 36)
    val name: Column<String> = varchar("NAME", 50).uniqueIndex()
    val image: Column<String> = varchar("IMAGE", 200)
}