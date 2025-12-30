package no.iktdev.streamit.service.db.tables.content

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.statements.InsertStatement

object SummaryTable : IntIdTable(name = "SUMMARY") {
    val description: Column<String> = text("DESCRIPTION")
    val language: Column<String> = varchar("LANGUAGE", 16)
    val cid: Column<Int> = integer("CATALOG_ID")

    init {
        uniqueIndex(language, cid)
    }

    fun insertIgnore(catalogId: Int, language: String, content: String): InsertStatement<Long> {
        return SummaryTable.insertIgnore {
            it[cid] = catalogId
            it[SummaryTable.language] = language
            it[description] = content
        }
    }
}