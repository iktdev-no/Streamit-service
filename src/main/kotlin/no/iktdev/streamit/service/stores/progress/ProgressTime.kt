package no.iktdev.streamit.service.stores.progress

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

internal fun Instant.toProgressDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(this, ZoneOffset.UTC)

internal fun LocalDateTime.toProgressInstant(): Instant =
    toInstant(ZoneOffset.UTC)
