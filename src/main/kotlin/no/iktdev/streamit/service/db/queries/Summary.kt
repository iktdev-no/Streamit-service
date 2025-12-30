package no.iktdev.streamit.service.db.queries

import no.iktdev.streamit.service.db.tables.content.SummaryTable
import no.iktdev.streamit.service.dto.Summary
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun SummaryTable.executeSelectOnId(id: Int): List<Summary> {
    return transaction {
        this@executeSelectOnId.selectAll().where {  cid eq id }
            .mapNotNull { Summary.fromRow(it) }
    }
}