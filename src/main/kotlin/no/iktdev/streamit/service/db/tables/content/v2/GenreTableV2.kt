package no.iktdev.streamit.service.db.tables.content.v2

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object GenreTableV2 : LongIdTable(name = "GENRE") {
    val genre: Column<String> = varchar("GENRE", 50)

    init {
        uniqueIndex(genre)
    }
}