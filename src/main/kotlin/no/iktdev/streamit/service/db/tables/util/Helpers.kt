package no.iktdev.streamit.service.db.tables.util

import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.Instant

fun LocalDateTime.toUtcInstant(): Instant = toInstant(ZoneOffset.UTC)

fun ResultRow?.exists(): Boolean {
    return this != null
}
