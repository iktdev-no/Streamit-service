package no.iktdev.streamit.service.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

data class Access(
    val username: String,
    val password: String,
    val address: String,
    val port: Int,
    val databaseName: String,
    val dbType: DatabaseTypes
) {}