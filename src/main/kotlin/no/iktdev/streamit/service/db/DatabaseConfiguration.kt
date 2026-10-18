package no.iktdev.streamit.service.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import jakarta.annotation.PostConstruct
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import java.sql.DriverManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
open class DatabaseConfiguration {

    @Bean
    fun dataSource(): DataSource {
        val maxPoolSize: Int = 10
        val access = DatabaseEnv.toAccess()

        ensureDatabaseExists(access)

        val jdbcUrl = when (access.dbType) {
            DatabaseTypes.MySQL -> "jdbc:mysql://${access.address}:${access.port}/${access.databaseName}?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC"
            DatabaseTypes.PostgreSQL -> "jdbc:postgresql://${access.address}:${access.port}/${access.databaseName}"
            DatabaseTypes.SQLite -> "jdbc:sqlite:${access.databaseName}.db"
            DatabaseTypes.H2 -> "jdbc:h2:mem:${access.databaseName};MODE=MySQL;DB_CLOSE_DELAY=-1"
        }

        val driver = when (access.dbType) {
            DatabaseTypes.MySQL -> "com.mysql.cj.jdbc.Driver"
            DatabaseTypes.PostgreSQL -> "org.postgresql.Driver"
            DatabaseTypes.SQLite -> "org.sqlite.JDBC"
            DatabaseTypes.H2 -> "org.h2.Driver"
        }

        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.driverClassName = driver
            this.username = access.username
            this.password = access.password
            this.maximumPoolSize = maxPoolSize
            this.isAutoCommit = false
            this.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            this.validate()
        }

        return HikariDataSource(config)
    }

    /** Create the configured schema before opening a pool whose JDBC URL targets it. */
    private fun ensureDatabaseExists(access: Access) {
        val (url, driver) = when (access.dbType) {
            DatabaseTypes.MySQL -> "jdbc:mysql://${access.address}:${access.port}/?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC" to "com.mysql.cj.jdbc.Driver"
            DatabaseTypes.PostgreSQL -> "jdbc:postgresql://${access.address}:${access.port}/postgres" to "org.postgresql.Driver"
            DatabaseTypes.SQLite, DatabaseTypes.H2 -> return
        }

        Class.forName(driver)
        DriverManager.getConnection(url, access.username, access.password).use { connection ->
            when (access.dbType) {
                DatabaseTypes.MySQL -> connection.createStatement().use { statement ->
                    val name = access.databaseName.replace("`", "``")
                    statement.execute("CREATE DATABASE IF NOT EXISTS `$name` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
                }
                DatabaseTypes.PostgreSQL -> {
                    val exists = connection.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?").use { statement ->
                        statement.setString(1, access.databaseName)
                        statement.executeQuery().use { it.next() }
                    }
                    if (!exists) {
                        val name = access.databaseName.replace("\"", "\"\"")
                        val owner = access.username.replace("\"", "\"\"")
                        connection.createStatement().use {
                            it.execute("CREATE DATABASE \"$name\" OWNER = \"$owner\"")
                        }
                    }
                }
                else -> Unit
            }
        }
        LoggerFactory.getLogger(DatabaseConfiguration::class.java)
            .info("Ensured database '{}' exists", access.databaseName)
    }

}

@Configuration
@EnableConfigurationProperties(FlywayProperties::class)
@ConditionalOnProperty(name = ["spring.flyway.enabled"], havingValue = "true", matchIfMissing = true)
class FlywayAutoConfig(
    private val dataSource: DataSource,
    private val props: FlywayProperties
) {

    private val log = LoggerFactory.getLogger(FlywayAutoConfig::class.java)

    @PostConstruct
    fun migrate() {
        val locations = props.locations.ifEmpty { listOf("classpath:flyway") }

        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(*locations.toTypedArray())
            .baselineOnMigrate(true)
            .load()

        val pending = flyway.info().pending()
        if (pending.isEmpty()) {
            log.warn("⚠️ No pending Flyway migrations found in ${locations.joinToString()}")
        } else {
            log.info("📦 Pending migrations: ${pending.joinToString { it.script }}")
        }

        flyway.migrate()
        log.info("✅ Flyway migration complete.")
    }
}

@ConfigurationProperties(prefix = "spring.flyway")
data class FlywayProperties(
    var locations: List<String> = emptyList()
)
