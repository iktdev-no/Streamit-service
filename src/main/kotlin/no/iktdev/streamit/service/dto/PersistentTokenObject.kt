package no.iktdev.streamit.service.dto

import java.time.LocalDateTime

data class PersistentTokenObject(
    val deviceId: String,
    val token: String,
    val createdAt: LocalDateTime,
    val revoked: Boolean
)