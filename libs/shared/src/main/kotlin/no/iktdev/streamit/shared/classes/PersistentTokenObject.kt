package no.iktdev.streamit.shared.classes

import java.time.LocalDateTime

data class PersistentTokenObject(
    val deviceId: String,
    val token: String,
    val createdAt: LocalDateTime,
    val revoked: Boolean
)