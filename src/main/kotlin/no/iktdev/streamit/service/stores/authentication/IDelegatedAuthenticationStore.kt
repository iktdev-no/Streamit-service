package no.iktdev.streamit.service.stores.authentication

import no.iktdev.streamit.service.db.tables.auth.DelegatedAuthenticationTable
import no.iktdev.streamit.service.model.internal.auth.DelegatedRequestData
import no.iktdev.streamit.service.model.internal.auth.InternalDelegatedRequestData
import no.iktdev.streamit.service.model.internal.auth.RequestDeviceInfo
import java.time.Instant

interface IDelegatedAuthenticationStore {
    fun createRequest(
        pin: String,
        requesterId: String,
        deviceInfo: RequestDeviceInfo,
        method: DelegatedAuthenticationTable.AuthMethod,
        ipAddress: String?
    ): Result<Instant>

    fun getPendingRequest(pin: String): DelegatedRequestData?
    fun getRequestStatus(pin: String, requesterId: String): InternalDelegatedRequestData?
    fun consumeIfPermitted(pin: String, requesterId: String, ipAddress: String?): Boolean
    fun permit(pin: String, requesterId: String): Boolean
}
