package no.iktdev.streamit.shared.database.queries

import no.iktdev.streamit.shared.database.AccessTokenTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun AccessTokenTable.executeInsertAndGetId(
    deviceId: String,
    token: String
) = transaction {
    AccessTokenTable.insertAndGetId {
        it[AccessTokenTable.deviceId] = deviceId
        it[AccessTokenTable.token] = token
    }
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