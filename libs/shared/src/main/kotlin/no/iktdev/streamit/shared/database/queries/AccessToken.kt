package no.iktdev.streamit.shared.database.queries

import no.iktdev.streamit.shared.database.AccessTokenTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

fun AccessTokenTable.executeInsertOrUpdate(
    deviceId: String,
    token: String
): String = transaction {
    val existing = AccessTokenTable.selectAll().where { AccessTokenTable.deviceId eq deviceId }.firstOrNull()
    if (existing != null) {
        AccessTokenTable.update({ AccessTokenTable.deviceId eq deviceId }) {
            it[AccessTokenTable.token] = token
        }
    } else {
        AccessTokenTable.insert {
            it[AccessTokenTable.deviceId] = deviceId
            it[AccessTokenTable.token] = token
        }
    }
    token
}

fun AccessTokenTable.executeGetTokenByDeviceId(
    deviceId: String
) = transaction {
    AccessTokenTable.selectAll().where { AccessTokenTable.deviceId eq deviceId }
        .firstOrNull()?.let {
            it[AccessTokenTable.token]
        }
}

fun AccessTokenTable.executeGetDeviceIdByToken(
    token: String
) = transaction {
    AccessTokenTable.selectAll().where { AccessTokenTable.token eq token }
        .firstOrNull()?.let {
            it[AccessTokenTable.deviceId]
        }
}

fun AccessTokenTable.isTokenRevoked(
    token: String
) = transaction {
    AccessTokenTable.selectAll().where { AccessTokenTable.token eq token }
        .firstOrNull()?.let {
            it[AccessTokenTable.revoked]
        } ?: false
}