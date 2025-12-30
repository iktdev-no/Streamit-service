package no.iktdev.streamit.service.db.tables.content

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object TitleTable: IntIdTable(name = "TITLES") {
    val masterTitle: Column<String> = varchar("MASTER_TITLE", 250)
    val alternativeTitle: Column<String> = varchar("ALTERNATIVE_TITLE", 250)

    init {
        uniqueIndex(masterTitle, alternativeTitle)
    }
}