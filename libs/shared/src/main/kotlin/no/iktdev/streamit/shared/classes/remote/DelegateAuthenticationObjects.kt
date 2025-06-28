package no.iktdev.streamit.shared.classes.remote

import no.iktdev.streamit.shared.toSHA256Hash
import java.time.LocalDateTime

data class AuthInitiateRequest(
    val pin: String,
    val deviceInfo: RequestDeviceInfo
) {
    fun toRequestId(): String = deviceInfo.toRequestId(true)
}

data class RequestDeviceInfo(
    val name: String,
    val model: String,
    val manufacturer: String,
    val clientOrOsVersion: String,
    val clientOrOsPlatform: String
) {
    fun toRequestId(withTimestamp: Boolean = false): String {
        val unhashed = arrayListOf(name, model, manufacturer, clientOrOsVersion, clientOrOsPlatform).joinToString("+")
        return if (withTimestamp)
            toSHA256Hash(unhashed + LocalDateTime.now().toString())
        else
            toSHA256Hash(unhashed)
    }
}

data class RequestCreatedResponse(
    val expiry: Long,
    val sessionId: String
)

data class PermitRequestData(
    val pin: String,
    val userId: String
)