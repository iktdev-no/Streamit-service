package no.iktdev.streamit.service.db.queries

import com.google.gson.Gson
import no.iktdev.streamit.service.db.tables.auth.DelegatedAuthenticationTable
import no.iktdev.streamit.service.db.tables.util.toUtcInstant
import no.iktdev.streamit.service.model.internal.auth.DelegatedRequestData
import no.iktdev.streamit.service.model.internal.auth.RequestDeviceInfo
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun DelegatedAuthenticationTable.executeInsertOrUpdate(
    pin: String,
    requestId: String,
    deviceInfo: String,
    method: DelegatedAuthenticationTable.AuthMethod,
    ip: String?
) = transaction {
    // TODO: Sette exires her hvis exposed ikke gjør det automatisk
    DelegatedAuthenticationTable.insertAndGetId {
        it[DelegatedAuthenticationTable.pin] = pin
        it[requesterId] = requestId
        it[DelegatedAuthenticationTable.deviceInfo] = deviceInfo
        it[DelegatedAuthenticationTable.method] = method
        it[ipaddress] = ip
    }
}

fun DelegatedAuthenticationTable.executeGetDelegatePendingRequestBy(pin: String) = transaction {
    DelegatedAuthenticationTable.selectAll().where { (DelegatedAuthenticationTable.pin eq pin) }.firstNotNullOfOrNull {
        DelegatedRequestData(
            pin = it[DelegatedAuthenticationTable.pin],
            requesterId = it[DelegatedAuthenticationTable.requesterId],
            deviceInfo = it[DelegatedAuthenticationTable.deviceInfo].let { json ->
                Gson().fromJson(
                    json,
                    RequestDeviceInfo::class.java
                )
            },
            created = it[DelegatedAuthenticationTable.created].toUtcInstant(),
            expires = it[DelegatedAuthenticationTable.expires].toUtcInstant(),
            permitted = it[DelegatedAuthenticationTable.permitted],
            consumed = it[DelegatedAuthenticationTable.consumed],
            method = it[DelegatedAuthenticationTable.method],
            ipaddress = it[DelegatedAuthenticationTable.ipaddress]
        )
    }
}
