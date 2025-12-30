package no.iktdev.streamit.service.db.tables.pfns

import no.iktdev.streamit.service.dto.auth.AccessTokenObject
import no.iktdev.streamit.service.toMD5
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object TokenTable :  IntIdTable(name = "TOKENS") {
    val tokenId = varchar("TOKEN_ID", 32).uniqueIndex()
    val token = text("TOKEN")
    val createdAt = datetime("CREATED_AT").clientDefault { LocalDateTime.now() }
    val revoked = bool("REVOKED").default(false)
    val revokeReason = text("REVOKED_REASON").nullable()

    fun findStateByToken(
        token: String
    ): AccessTokenObject?  = transaction {
        val tokenId = token.toMD5()
        TokenTable.selectAll().where { TokenTable.tokenId eq tokenId }
            .firstOrNull()?.let {
                AccessTokenObject(
                    it[TokenTable.token],
                    it[createdAt],
                    it[revoked],
                    it[revokeReason]
                )
            }
    }

    fun insertToken(token: String): Boolean = transaction {
        val tokenId = token.toMD5()
        if (tokenIdExists(tokenId)) return@transaction true

        try {
            TokenTable.insert {
                it[TokenTable.tokenId] = tokenId
                it[TokenTable.token] = token
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun tokenIdExists(tokenId: String): Boolean = transaction {
        !TokenTable.select(TokenTable.tokenId).where { TokenTable.tokenId eq tokenId }.empty()
    }

}