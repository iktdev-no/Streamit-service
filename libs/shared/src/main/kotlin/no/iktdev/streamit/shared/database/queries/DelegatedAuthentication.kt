package no.iktdev.streamit.shared.database.queries

import com.google.gson.Gson
import no.iktdev.streamit.library.db.tables.authentication.DelegatedAuthenticationTable
import no.iktdev.streamit.library.db.toEpochSeconds
import no.iktdev.streamit.shared.classes.remote.DelegatedRequestData
import no.iktdev.streamit.shared.classes.remote.RequestDeviceInfo
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun DelegatedAuthenticationTable.executeInsertAndGetId(
    pin: String,
    requestId: String,
    deviceInfo: String,
    method: DelegatedAuthenticationTable.AuthMethod,
    ip: String?
) = transaction {
    DelegatedAuthenticationTable.insertAndGetId {
        it[DelegatedAuthenticationTable.pin] = pin
        it[requesterId] = requestId
        it[DelegatedAuthenticationTable.deviceInfo] = deviceInfo
        it[DelegatedAuthenticationTable.method] = method
        it[ipaddress] = ip
    }
}

fun DelegatedAuthenticationTable.executeGetDelegatePendingRequestBy(pin: String) = transaction {
    DelegatedAuthenticationTable.select { DelegatedAuthenticationTable.pin eq pin }.firstNotNullOfOrNull {
        DelegatedRequestData(
            pin = it[DelegatedAuthenticationTable.pin],
            requesterId = it[DelegatedAuthenticationTable.requesterId],
            deviceInfo = it[DelegatedAuthenticationTable.deviceInfo].let { json ->
                Gson().fromJson(
                    json,
                    RequestDeviceInfo::class.java
                )
            },
            created = it[DelegatedAuthenticationTable.created].toEpochSeconds(),
            expires = it[DelegatedAuthenticationTable.expires].toEpochSeconds(),
            permitted = it[DelegatedAuthenticationTable.permitted],
            consumed = it[DelegatedAuthenticationTable.consumed],
            method = it[DelegatedAuthenticationTable.method],
            ipaddress = it[DelegatedAuthenticationTable.ipaddress]
        )
    }
}