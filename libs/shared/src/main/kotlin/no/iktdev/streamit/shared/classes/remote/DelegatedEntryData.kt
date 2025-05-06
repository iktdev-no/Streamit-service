package no.iktdev.streamit.shared.classes.remote

import no.iktdev.streamit.library.db.tables.authentication.DelegatedAuthenticationTable
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