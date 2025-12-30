package no.iktdev.streamit.service.db

import mu.KotlinLogging
import no.iktdev.streamit.service.db.tables.util.withTransaction
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.statements.jdbc.JdbcConnectionImpl
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class FlywayMigrationTest {

    private val log = KotlinLogging.logger {}


    @Test
    fun `should run flyway migrations and create expected tables`() {
        val access = Access(
            username = "sa",
            password = "",
            address = "", // ikke brukt for H2
            port = 0,     // ikke brukt for H2
            databaseName = "testdb",
            dbType = DbType.H2
        )

        val connection = DatabaseConfig.connect(access)

        val flyway = Flyway.configure()
            .dataSource(connection.second)
            .locations("classpath:flyway")
            .cleanDisabled(false)
            .baselineOnMigrate(true)
            .load()

        // Flyway migrering
        flyway.clean()
        flyway.migrate()

        // Verifiser at tabellene finnes

        withTransaction {
            val jdbc = (TransactionManager.current().connection as JdbcConnectionImpl).connection

            val meta = jdbc.metaData
            val tableNames = listOf<String>(
                "PERSISTENT_TOKENS",
                "TOKENS",

                "DELEGATED_AUTHENTICATION",
                "REGISTERED_DEVICES",
                "CATALOG",
                "CONTINUE_WATCH",
                "FAVORITES",
                "GENRE",
                "MOVIE",
                "PROGRESS",
                "SERIE",
                "SUBTITLE",
                "SUMMARY",
                "TITLES",
                "CAST_ERROR",
                "USERS",
                "PROFILE_IMAGE",
                "MEDIA_VIDEO_DATA",
                "MEDIA_DATA_AUDIO"
            )
            val existingTables = tableNames.map {
                it to meta.getTables(null, null, it.uppercase(), null).next()
            }

            existingTables.forEach { (tableName, exists) ->
                assertTrue(exists, "Table $tableName should exist after migration")
            }

            log.info { "Found migrations: ${flyway.info().all().map { it.script }}" }
        }
    }
}
