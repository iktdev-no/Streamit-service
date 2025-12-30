package no.iktdev.streamit.service.dto.auth

import java.time.LocalDateTime

data class AccessTokenObject(
    val token: String,
    val createdAt: LocalDateTime,
    val revoked: Boolean,
    val revokeReason: String? = null
)