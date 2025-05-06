package no.iktdev.streamit.shared.classes.remote

import no.iktdev.streamit.shared.toSHA256Hash
import java.time.LocalDateTime

data class AuthInitiateRequest(
    val pin: String,
    val deviceInfo: RequestDeviceInfo
) {
    fun toRequestId(): String {
        val unhashed = arrayListOf(deviceInfo.name, deviceInfo.model, deviceInfo.manufacturer, deviceInfo.clientOrOsVersion, deviceInfo.clientOrOsPlatform).joinToString("+")
        return toSHA256Hash(unhashed + LocalDateTime.now().toString())
    }
}

data class RequestDeviceInfo(
    val name: String,
    val model: String,
    val manufacturer: String,
    val clientOrOsVersion: String,
    val clientOrOsPlatform: String
)

data class RequestCreatedResponse(
    val expiry: Long,
    val sessionId: String
)

data class PermitRequestData(
    val pin: String,
    val userId: String
)