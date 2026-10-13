package no.iktdev.streamit.service.db.tables.content.v2

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object VideoTableV2 : LongIdTable(name = "VIDEO") {
    val file: Column<String> = varchar("FILE", 500)
    val duration: Column<Long?> = long("DURATION").nullable()
    val addedAt: Column<LocalDateTime> = datetime("ADDED_AT")
}