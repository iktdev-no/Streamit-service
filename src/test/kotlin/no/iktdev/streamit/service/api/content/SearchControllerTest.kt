package no.iktdev.streamit.service.api.content

import no.iktdev.streamit.library.db.tables.content.CatalogTable
import no.iktdev.streamit.library.db.tables.content.GenreTable
import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.asList
import no.iktdev.streamit.service.assertHttpOk
import no.iktdev.streamit.service.assertJson
import no.iktdev.streamit.service.simpleGet
import no.iktdev.streamit.shared.classes.Catalog
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference

class SearchControllerTest: TestBaseWithDatabase() {
    @Autowired
    lateinit var restTemplate: TestRestTemplate


    @BeforeAll
    fun insertCatalogContent() {
        transaction {
            GenreTable.insert {
                it[GenreTable.id] = 1
                it[GenreTable.genre] = "Test"
            }
            CatalogTable.insertMovie(
                title = "Potetmonsteret",
                collection = "Potetmonsteret",
                cover = "Potetmonsteret.jpg",
                genres = "1",
                videoFile = "Potetmonsteret.mp4"
            )
            CatalogTable.insertMovie(
                title = "Gulrotspøkelset",
                collection = "Gulrotspøkelset",
                cover = "Gulrotspøkelset.jpg",
                genres = "1",
                videoFile = "Gulrotspøkelset.mp4"
            )

            CatalogTable.insertMovie(
                title = "Epleskurken",
                collection = "Epleskurken",
                cover = "Epleskurken.jpg",
                genres = "1",
                videoFile = "Epleskurken.mp4"
            )

            CatalogTable.insertMovie(
                title = "Tomattrøbbel",
                collection = "Tomattrøbbel",
                cover = "Tomattrøbbel.jpg",
                genres = "1",
                videoFile = "Tomattrøbbel.mp4"
            )
        }
    }

    @Test
    fun `Search should return single item`() {
        val response = restTemplate.simpleGet("/open/api/search/Potet",
            object : ParameterizedTypeReference<List<Catalog>>() {}
        )
        assertHttpOk(response)
        assertJson(
            //language=json
            """
                [
                    {
                        "id": 1,
                        "title": "Potetmonsteret",
                        "collection": "Potetmonsteret",
                        "cover": "Potetmonsteret.jpg"
                    }
                ]
        """.trimIndent(), response.body)
    }
}