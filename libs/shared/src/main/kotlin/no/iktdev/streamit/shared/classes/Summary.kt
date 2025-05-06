package no.iktdev.streamit.shared.classes

import no.iktdev.streamit.library.db.tables.content.SummaryTable
import org.jetbrains.exposed.sql.ResultRow

data class Summary(val id: Int, val description: String, val language: String, val cid: Int)
{
    companion object
    {
        fun fromRow(row: ResultRow) = Summary(
            id = row[SummaryTable.id].value,
            description = row[SummaryTable.description],
            language = row[SummaryTable.language],
            cid = row[SummaryTable.cid]
        )
    }
}