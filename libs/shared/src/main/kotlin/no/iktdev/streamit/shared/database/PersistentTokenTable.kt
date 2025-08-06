package no.iktdev.streamit.shared.database

import no.iktdev.streamit.shared.toMD5
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object PersistentTokenTable : IntIdTable(name = "PersistentTokens") {
    val deviceId = varchar("deviceId", 70).uniqueIndex()
    val tokenId = reference("tokenId", TokenTable.tokenId)

    init {
        uniqueIndex(deviceId, tokenId)
    }

    fun executeInsertOrUpdate(deviceId: String, token: String): String? = transaction {
        val tokenId = token.toMD5()
        val existsInPersist = PersistentTokenTable.selectAll()
            .where { PersistentTokenTable.deviceId.eq(deviceId) or PersistentTokenTable.tokenId.eq(tokenId) }.firstOrNull()

        if (existsInPersist.exists()) {
            PersistentTokenTable.update({ PersistentTokenTable.deviceId.eq(deviceId) or PersistentTokenTable.tokenId.eq(tokenId)}) {
                it[PersistentTokenTable.tokenId] = tokenId
                it[PersistentTokenTable.deviceId] = PersistentTokenTable.deviceId
            }
        } else {
            TokenTable.insertToken(token)

            PersistentTokenTable.insert {
                it[PersistentTokenTable.deviceId] = deviceId
                it[PersistentTokenTable.tokenId] = tokenId
            }


        }

        token
    }
}