package no.iktdev.streamit.service.api.content

import no.iktdev.streamit.library.db.tables.content.CatalogTable
import no.iktdev.streamit.library.db.tables.content.GenreTable
import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.asList
import no.iktdev.streamit.service.assertHttpOk
import no.iktdev.streamit.service.assertJson
import no.iktdev.streamit.service.simpleGet
import no.iktdev.streamit.shared.classes.Catalog
import no.iktdev.streamit.shared.classes.GenreCatalog
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference

class CatalogControllerTest: TestBaseWithDatabase() {
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
    fun `Catalog should return a list of entries`() {
        val response = restTemplate.simpleGet("/open/api/catalog",
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
        """.trimIndent(), response.body.first().asList())
    }

    @Test
    fun `Catalog should return a list of movies`() {
        val response = restTemplate.simpleGet("/open/api/catalog/movie",
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
        """.trimIndent(), response.body.first().asList())
    }

    @Test
    fun `Catalog should return a list of genre grouped elements`() {
        val response = restTemplate.simpleGet("/open/api/catalog/genre",
            object : ParameterizedTypeReference<List<GenreCatalog>>() {}
        )
        assertHttpOk(response)
        assertJson(
            //language=json
            """
                [
                  {
                    "genre": {
                      "id": 1,
                      "genre": "Test"
                    },
                    "catalog": [
                        {
                            "id": 1,
                            "title": "Potetmonsteret",
                            "collection": "Potetmonsteret",
                            "cover": "Potetmonsteret.jpg"
                        } 
                    ]
                  }
                ]
        """.trimIndent(), response.body.map { it.apply {
            val new = it.catalog.first()
            this.catalog.clear()
                this.catalog.add(new)
            } })
    }



}
