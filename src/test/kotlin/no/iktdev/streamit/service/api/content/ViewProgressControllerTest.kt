package no.iktdev.streamit.service.api.content

import no.iktdev.streamit.library.db.tables.content.CatalogTable
import no.iktdev.streamit.library.db.tables.content.GenreTable
import no.iktdev.streamit.library.db.tables.content.ProgressTable
import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.asList
import no.iktdev.streamit.service.assertJson
import no.iktdev.streamit.service.simpleGet
import no.iktdev.streamit.shared.classes.Catalog
import no.iktdev.streamit.shared.classes.Serie
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import java.time.LocalDateTime
import java.util.UUID

class ViewProgressControllerTest(): TestBaseWithDatabase() {
    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @BeforeAll
    fun insertGeneralData() {
        transaction {
            CatalogTable.insertMovie(
                title = "Interruption",
                collection = "interruption",
                cover = "interruption.jpg",
                genres = "Psychological, Comedy",
                videoFile = "interruption_final.mp4"
            )

            // Sett inn serier
            CatalogTable.insertSerie(
                title = "Baking Bread",
                collection = "baking_bread",
                cover = "baking_bread.jpg",
                genres = "Crumbs, Drama",
                videoFile = "bb_baguette01.mp4",
                episode = 1,
                season = 1
            )

            CatalogTable.insertSerie(
                title = "Baking Bread",
                collection = "baking_bread",
                cover = "baking_bread.jpg",
                genres = "Crumbs, Drama",
                videoFile = "bb_doughdisaster02.mp4",
                episode = 2,
                season = 1
            )

            CatalogTable.insertSerie(
                title = "Baking Bread",
                collection = "baking_bread",
                cover = "baking_bread.jpg",
                genres = "Crumbs, Drama",
                videoFile = "bb_risingtension03.mp4",
                episode = 3,
                season = 1
            )
        }
    }

    @AfterEach
    fun clearProgress() {
        transaction {
            ProgressTable.deleteAll()
        }
    }

    @Test
    @DisplayName("""
        Given a normal viewing pattern,
        There should be no episode returned,
        When the last episode is fully viewed and there is no next episode
    """)
    fun scenario1() {
        val userId = UUID.randomUUID().toString()
        transaction {
            ProgressTable.insert {
                it[guid] = userId
                it[type] = "serie"
                it[title] = "Baking Bread"
                it[collection] = "baking_bread"
                it[episode] = 1
                it[season] = 1
                it[video] = "bb_baguette01.mp4"
                it[progress] = 5  // Nettopp startet
                it[duration] = 50
                it[played] = LocalDateTime.now()
            }

            ProgressTable.insert {
                it[guid] = userId
                it[type] = "serie"
                it[title] = "Baking Bread"
                it[collection] = "baking_bread"
                it[episode] = 2
                it[season] = 1
                it[video] = "bb_doughdisaster02.mp4"
                it[progress] = 45  // Nesten ferdig
                it[duration] = 50
                it[played] = LocalDateTime.now()
            }

            ProgressTable.insert {
                it[guid] = userId
                it[type] = "serie"
                it[title] = "Baking Bread"
                it[collection] = "baking_bread"
                it[episode] = 3
                it[season] = 1
                it[video] = "bb_risingtension03.mp4"
                it[progress] = 50  // Ferdig sett
                it[duration] = 50
                it[played] = LocalDateTime.now()
            }
        }
        val response = restTemplate.simpleGet("/api/open/progress/$userId/continue/serie",
            object : ParameterizedTypeReference<List<Serie>>() {}
        )
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

}