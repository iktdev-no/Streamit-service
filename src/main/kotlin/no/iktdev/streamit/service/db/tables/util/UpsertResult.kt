package no.iktdev.streamit.service.db.tables.util

import org.jetbrains.exposed.sql.statements.InsertStatement

sealed class UpsertResult {
    data class Inserted(val statement: InsertStatement<Long>) : UpsertResult()
    object Updated : UpsertResult()
    object Skipped : UpsertResult() // hvis du ønsker en tredje variant
}