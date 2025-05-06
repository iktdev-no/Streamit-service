package no.iktdev.streamit.service

import no.iktdev.streamit.library.db.withDirtyRead
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestBaseWithDatabase: TestBase() {
    lateinit var database: Database

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
}