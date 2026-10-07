package no.iktdev.streamit.service.stores.authentication

import com.google.gson.Gson
import no.iktdev.streamit.service.db.tables.auth.DelegatedAuthenticationTable
import no.iktdev.streamit.service.db.tables.util.toUtcInstant
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.model.internal.auth.DelegatedRequestData
import no.iktdev.streamit.service.model.internal.auth.InternalDelegatedRequestData
import no.iktdev.streamit.service.model.internal.auth.RequestDeviceInfo
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime

@Component
class DelegatedAuthenticationStore : IDelegatedAuthenticationStore {
    private val gson = Gson()

    override fun createRequest(
        pin: String,
        requesterId: String,
        deviceInfo: RequestDeviceInfo,
        method: DelegatedAuthenticationTable.AuthMethod,
        ipAddress: String?
    ): Result<Instant> = withTransaction(rollbackOnFailure = true) {
        val id = DelegatedAuthenticationTable.insertAndGetId {
            it[DelegatedAuthenticationTable.pin] = pin
            it[DelegatedAuthenticationTable.requesterId] = requesterId
            it[DelegatedAuthenticationTable.deviceInfo] = gson.toJson(deviceInfo)
            it[DelegatedAuthenticationTable.method] = method
            it[DelegatedAuthenticationTable.ipaddress] = ipAddress
        }

        DelegatedAuthenticationTable
            .selectAll()
            .where { DelegatedAuthenticationTable.id eq id }
            .single()[DelegatedAuthenticationTable.expires]
            .toUtcInstant()
    }

    override fun getPendingRequest(pin: String): DelegatedRequestData? = withTransaction(db = null, run = {
        DelegatedAuthenticationTable
            .selectAll()
            .where { DelegatedAuthenticationTable.pin eq pin }
            .firstOrNull()
            ?.let { row ->
                DelegatedRequestData(
                    pin = row[DelegatedAuthenticationTable.pin],
                    requesterId = row[DelegatedAuthenticationTable.requesterId],
                    deviceInfo = gson.fromJson(
                        row[DelegatedAuthenticationTable.deviceInfo],
                        RequestDeviceInfo::class.java
                    ),
                    created = row[DelegatedAuthenticationTable.created].toUtcInstant(),
                    expires = row[DelegatedAuthenticationTable.expires].toUtcInstant(),
                    permitted = row[DelegatedAuthenticationTable.permitted],
                    consumed = row[DelegatedAuthenticationTable.consumed],
                    method = row[DelegatedAuthenticationTable.method],
                    ipaddress = row[DelegatedAuthenticationTable.ipaddress]
                )
            }
    })

    override fun getRequestStatus(pin: String, requesterId: String): InternalDelegatedRequestData? =
        withTransaction(db = null, run = {
            DelegatedAuthenticationTable
                .selectAll()
                .where {
                    (DelegatedAuthenticationTable.pin eq pin) and
                        (DelegatedAuthenticationTable.requesterId eq requesterId)
                }
                .singleOrNull()
                ?.let { row ->
                    InternalDelegatedRequestData(
                        pin = row[DelegatedAuthenticationTable.pin],
                        requesterId = row[DelegatedAuthenticationTable.requesterId],
                        created = row[DelegatedAuthenticationTable.created],
                        expires = row[DelegatedAuthenticationTable.expires],
                        permitted = row[DelegatedAuthenticationTable.permitted],
                        consumed = row[DelegatedAuthenticationTable.consumed],
                        ipaddress = row[DelegatedAuthenticationTable.ipaddress]
                    )
                }
        })

    override fun consumeIfPermitted(pin: String, requesterId: String, ipAddress: String?): Boolean =
        withTransaction(rollbackOnFailure = true) {
            val requestIdentity =
                (DelegatedAuthenticationTable.pin eq pin) and
                    (DelegatedAuthenticationTable.requesterId eq requesterId)
            val matchingIp = if (ipAddress == null) {
                DelegatedAuthenticationTable.ipaddress.isNull()
            } else {
                DelegatedAuthenticationTable.ipaddress eq ipAddress
            }

            DelegatedAuthenticationTable.update({
                requestIdentity and matchingIp and
                    (DelegatedAuthenticationTable.permitted eq true) and
                    (DelegatedAuthenticationTable.consumed eq false) and
                    (DelegatedAuthenticationTable.expires greater LocalDateTime.now())
            }) {
                it[consumed] = true
            } == 1
        }.getOrDefault(false)

    override fun permit(pin: String, requesterId: String): Boolean =
        withTransaction(rollbackOnFailure = true) {
            DelegatedAuthenticationTable.update({
                (DelegatedAuthenticationTable.pin eq pin) and
                    (DelegatedAuthenticationTable.requesterId eq requesterId)
            }) {
                it[permitted] = true
            } > 0
        }.getOrDefault(false)
}
