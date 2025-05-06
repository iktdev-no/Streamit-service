package no.iktdev.streamit.shared.database.queries

import no.iktdev.streamit.shared.classes.Summary
import no.iktdev.streamit.library.db.tables.content.SummaryTable
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun SummaryTable.executeSelectOnId(id: Int): List<Summary> {
    return transaction {
        this@executeSelectOnId.select { cid eq id }
            .mapNotNull { Summary.fromRow(it) }
    }
}