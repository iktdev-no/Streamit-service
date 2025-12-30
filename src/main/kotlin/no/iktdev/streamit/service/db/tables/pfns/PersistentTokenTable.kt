package no.iktdev.streamit.service.db.tables.pfns

import no.iktdev.streamit.service.db.tables.util.exists
import no.iktdev.streamit.service.toMD5
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object PersistentTokenTable : IntIdTable(name = "PERSISTENT_TOKENS") {
    val deviceId = varchar("DEVICE_ID", 70).uniqueIndex()
    val tokenId = reference("TOKEN_ID", TokenTable.tokenId)

    init {
        uniqueIndex(deviceId, tokenId)
    }

    fun executeInsertOrUpdate(deviceId: String, token: String): String? = transaction {
        val tokenId = token.toMD5()
        val existsInPersist = PersistentTokenTable.selectAll()
            .where { PersistentTokenTable.deviceId.eq(deviceId) or PersistentTokenTable.tokenId.eq(tokenId) }.firstOrNull()

        val existsInTokens = TokenTable.tokenIdExists(tokenId)
        val permitOperationsOnTable = existsInTokens || TokenTable.insertToken(token)

        if (!permitOperationsOnTable) {
            throw RuntimeException("Unable to insert token into token table")
        }

        if (existsInPersist.exists()) {
            PersistentTokenTable.update({ PersistentTokenTable.deviceId.eq(deviceId) or PersistentTokenTable.tokenId.eq(tokenId)}) {
                it[PersistentTokenTable.tokenId] = tokenId
                it[PersistentTokenTable.deviceId] = deviceId
            }
        } else {
            PersistentTokenTable.insert {
                it[PersistentTokenTable.deviceId] = deviceId
                it[PersistentTokenTable.tokenId] = tokenId
            }
        }
        token
    }
}