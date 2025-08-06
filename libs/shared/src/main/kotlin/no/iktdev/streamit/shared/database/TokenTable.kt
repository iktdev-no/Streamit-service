package no.iktdev.streamit.shared.database

import no.iktdev.streamit.shared.classes.AccessTokenObject
import no.iktdev.streamit.shared.toMD5
import no.iktdev.streamit.shared.toSHA256Hash
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object TokenTable :  IntIdTable(name = "Tokens") {
    val tokenId = varchar("tokenId", 32).uniqueIndex()
    val token = text("token")
    val createdAt = datetime("created").clientDefault { LocalDateTime.now() }
    val revoked = bool("revoked").default(false)
    val revokeReason = text("revokeReason").nullable()

    fun findStateByToken(
        token: String
    ): AccessTokenObject?  = transaction {
        val tokenId = token.toMD5()
        TokenTable.selectAll().where { TokenTable.tokenId eq tokenId }
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
            it[TokenTable.tokenId] = token.toMD5()
            it[TokenTable.token] = token
        }
        rows.insertedCount == 1
    }
}