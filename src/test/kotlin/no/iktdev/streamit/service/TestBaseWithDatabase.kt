package no.iktdev.streamit.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.iktdev.streamit.library.db.withDirtyRead
import no.iktdev.streamit.library.db.withTransaction
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestBaseWithDatabase: TestBase() {
    lateinit var database: Database

    val mapper = ObjectMapper()
    val ds = H2Datasource("test")

    @BeforeAll
    fun setupDatabase() {
        database = ds.toDatabase2()
        System.out.println(ds)
        databaseSetup(database)
    }

    @BeforeAll
    fun verifyDatabaseSetup() {
        withDirtyRead(database) {
            getTables().forEach { table ->
                assertThat(table.exists()).isTrue
            }
        }
    }

    @AfterAll
    fun clearDatabase() {
        withTransaction {
            getTables().forEach { table ->
                SchemaUtils.drop(table)
            }
        }
    }
}