package no.iktdev.streamit.service.api.content

import no.iktdev.streamit.library.db.tables.content.CatalogTable
import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.shared.classes.Catalog
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class CatalogControllerTest: TestBaseWithDatabase() {
    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @BeforeAll
    fun insertCatalogContent() {
        transaction {
            CatalogTable.insertMovie(
                title = "Potetmonsteret",
                collection = "Potetmonsteret",
                cover = "Potetmonsteret.jpg",
                genres = null,
                videoFile = "Potetmonsteret.mp4"
            )
        }
    }

    @Test
    fun `open accessible endpoint should return OK`() {
        val headers = HttpHeaders().apply {
        }
        val request = HttpEntity<Void>(headers)

        val response = restTemplate.exchange(
            "/api/open/catalog/", // 👈 bare path
            HttpMethod.GET,
            request,
            object : ParameterizedTypeReference<List<Catalog>>() {}
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }
}