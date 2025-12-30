package no.iktdev.streamit.service.db.tables.auth

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object DelegatedAuthenticationTable: IntIdTable(name = "DELEGATED_AUTHENTICATION") {
    val pin: Column<String> = varchar("PIN", 8)
    val requesterId: Column<String> = char("REQUESTER_ID", 64)
    val deviceInfo: Column<String> = varchar("DEVICE_INFO", 256)
    val created: Column<LocalDateTime> = datetime("CREATED_AT").clientDefault { LocalDateTime.now() }
    val expires: Column<LocalDateTime> = datetime("EXPIRES_AT").clientDefault {  LocalDateTime.now().plusMinutes(15) }
    val permitted: Column<Boolean> = bool("PERMITTED").default(false)
    val consumed: Column<Boolean> = bool("CONSUMED").default(false)
    val method = enumerationByName("METHOD", 3, AuthMethod::class) // Brukt metode (PIN eller QR)
    val ipaddress: Column<String?> = varchar("IP_ADDRESS", 39).nullable()

    init {
        uniqueIndex(pin)
    }

    enum class AuthMethod {
        PIN, QR
    }
}

