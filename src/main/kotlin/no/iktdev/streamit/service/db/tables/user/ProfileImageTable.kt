package no.iktdev.streamit.service.db.tables.user

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object ProfileImageTable : IntIdTable(name = "PROFILE_IMAGE") {
    val filename: Column<String> = varchar("FILE_NAME", 250)
    val added: Column<LocalDateTime> = datetime("ADDED_AT").defaultExpression(CurrentDateTime)
}