package no.iktdev.streamit.service.model.internal.auth

import java.time.LocalDateTime

data class AccessTokenObject(
    val token: String,
    val createdAt: LocalDateTime,
    val revoked: Boolean,
    val revokeReason: String? = null
)