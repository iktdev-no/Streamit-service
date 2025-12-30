package no.iktdev.streamit.service.dto.auth

import no.iktdev.streamit.service.db.tables.auth.DelegatedAuthenticationTable
import java.time.LocalDateTime


data class DelegatedRequestData(
    val requesterId: String,
    val pin: String,
    val deviceInfo: RequestDeviceInfo,
    val created: Long,
    val expires: Long,
    val permitted: Boolean,
    val consumed: Boolean,
    val method: DelegatedAuthenticationTable.AuthMethod,
    val ipaddress: String?
)

data class InternalDelegatedRequestData(
    val requesterId: String,
    val pin: String,
    val created: LocalDateTime,
    val expires: LocalDateTime,
    val permitted: Boolean,
    val consumed: Boolean,
    val ipaddress: String?
)