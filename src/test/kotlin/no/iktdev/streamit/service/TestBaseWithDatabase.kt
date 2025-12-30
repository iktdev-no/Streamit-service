package no.iktdev.streamit.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.iktdev.streamit.service.auth.Authentication
import no.iktdev.streamit.service.db.Access
import no.iktdev.streamit.service.db.DbType
import no.iktdev.streamit.service.db.tables.util.withTransaction
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.statements.jdbc.JdbcConnectionImpl
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import javax.sql.DataSource
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class TestBaseWithDatabase: TestBase() {

    init {
        mockFolders()
    }

    var validToken: String? = null
    val mapper = ObjectMapper()


    @Autowired
    lateinit var dataSource: DataSource

    lateinit var database: Database
    private lateinit var flyway: Flyway


    @BeforeAll
    fun setupDatabase() {
        val access = Access(
            username = "sa",
            password = "",
            address = "", // ikke brukt for H2
            port = 0,     // ikke brukt for H2
            databaseName = "testdb",
            dbType = DbType.H2
        )
        database = Database.connect(dataSource)
        flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:flyway")
            .cleanDisabled(false)
            .load()

        flyway.clean()
        flyway.migrate()

        validToken = Authentication().createJwt()
        assertNotNull(validToken)


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

    @AfterAll
    fun clearDatabase() {
        flyway.clean()
        TransactionManager.closeAndUnregister(database)
    }



}
