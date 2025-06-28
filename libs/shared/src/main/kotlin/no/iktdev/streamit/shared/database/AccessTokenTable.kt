package no.iktdev.streamit.shared.database

import no.iktdev.streamit.library.db.tables.authentication.DelegatedAuthenticationTable.pin
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object AccessTokenTable: IntIdTable(name = "AccessToken") {
    val deviceId = varchar("deviceId", 70).uniqueIndex()
    val token = varchar("token", 1024).uniqueIndex()
    val createdAt = datetime("created").clientDefault { LocalDateTime.now() }
    val revoked = bool("revoked").default(false)

    init {
        uniqueIndex(deviceId, token)
    }
}