package no.iktdev.streamit.shared.database

import no.iktdev.streamit.shared.classes.AccessTokenObject
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object TokenTable :  IntIdTable(name = "Token") {
    val token = varchar("token", 1024).uniqueIndex()
    val createdAt = datetime("created").clientDefault { LocalDateTime.now() }
    val revoked = bool("revoked").default(false)
    val revokeReason = text("revokeReason").nullable()

    fun findStateByToken(
        token: String
    ): AccessTokenObject?  = transaction {
        TokenTable.selectAll().where { TokenTable.token eq token }
            .firstOrNull()?.let {
                AccessTokenObject(
                    it[TokenTable.token],
                    it[TokenTable.createdAt],
                    it[TokenTable.revoked],
                    it[TokenTable.revokeReason]
                )
            }
    }

    fun insertToken(token: String): Boolean = transaction {
        val rows = TokenTable.insert {
            it[TokenTable.token] = token
        }
        rows.insertedCount == 1
    }
}