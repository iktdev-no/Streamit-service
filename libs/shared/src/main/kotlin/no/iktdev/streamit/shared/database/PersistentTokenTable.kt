package no.iktdev.streamit.shared.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

object PersistentTokenTable : IntIdTable(name = "PersistentTokens") {
    val deviceId = varchar("deviceId", 70).uniqueIndex()
    val token = reference("token", TokenTable.token)

    init {
        uniqueIndex(deviceId, token)
    }

    fun executeInsertOrUpdate(deviceId: String, token: String): String? = transaction {
        val existsInPersist = PersistentTokenTable.selectAll()
            .where { PersistentTokenTable.deviceId.eq(deviceId) or PersistentTokenTable.token.eq(token) }.firstOrNull()

        if (existsInPersist.exists()) {
            PersistentTokenTable.update({ PersistentTokenTable.deviceId.eq(deviceId) or PersistentTokenTable.token.eq(token)}) {
                it[PersistentTokenTable.token] = token
                it[PersistentTokenTable.deviceId] = PersistentTokenTable.deviceId
            }
        } else {
            TokenTable.insertIgnore {
                it[TokenTable.token] = token
            }

            PersistentTokenTable.insert {
                it[PersistentTokenTable.deviceId] = deviceId
                it[PersistentTokenTable.token] = token
            }


        }

        token
    }
}