package no.iktdev.streamit.service.api.content

import no.iktdev.streamit.library.db.tables.content.CatalogTable
import no.iktdev.streamit.library.db.tables.content.GenreTable
import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.assertHttpOk
import no.iktdev.streamit.service.assertJson
import no.iktdev.streamit.service.simpleGet
import no.iktdev.streamit.shared.classes.Catalog
import no.iktdev.streamit.shared.classes.Genre
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference

class GenreControllerTest: TestBaseWithDatabase() {
    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @BeforeAll
    fun insertCatalogContent() {
        transaction {
            GenreTable.insert {
                it[GenreTable.genre] = "Test"
            }
        }
    }

    @Test
    fun `Genres should return a list of entries`() {
        val response = restTemplate.simpleGet("/api/open/genre",
            object : ParameterizedTypeReference<List<Genre>>() {}
        )
        assertHttpOk(response)
        assertJson(
            //language=json
            """
                [
                    {
                        "id": 1,
                        "genre": "Test"
                    }
                ]
        """.trimIndent(), response.body)
    }

}