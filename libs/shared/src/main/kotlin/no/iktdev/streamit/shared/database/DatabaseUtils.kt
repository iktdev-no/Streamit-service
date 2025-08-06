package no.iktdev.streamit.shared.database

import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

fun timestampToLocalDateTime(timestamp: Int): LocalDateTime {
    return Instant.ofEpochSecond(timestamp.toLong()).atZone(ZoneId.systemDefault()).toLocalDateTime()
}

fun LocalDateTime.toEpochSeconds(): Long {
    return this.toEpochSecond(ZoneOffset.ofTotalSeconds(ZoneOffset.systemDefault().rules.getOffset(LocalDateTime.now()).totalSeconds))
}

fun ResultRow?.exists(): Boolean {
    return this != null
}