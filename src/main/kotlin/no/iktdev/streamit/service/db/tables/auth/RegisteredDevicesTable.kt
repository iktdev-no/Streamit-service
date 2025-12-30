package no.iktdev.streamit.service.db.tables.auth

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object RegisteredDevicesTable : IntIdTable(name = "REGISTERED_DEVICES") {
    val deviceId: Column<String> = varchar("DEVICE_ID", 256)
    val applicationPackageName: Column<String> = varchar("APPLICATION_PACKAGE_NAME", 32)
    val osVersion: Column<String> = varchar("OS_VERSION", 28)
    val osPlatform: Column<String> = varchar("OS_PLATFORM", 28)
    val registered: Column<LocalDateTime> = datetime("REGISTERED_AT").defaultExpression(CurrentDateTime)
    val lastSeen: Column<LocalDateTime> = datetime("LAST_SEEN_AT").defaultExpression(CurrentDateTime)
}