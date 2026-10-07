package no.iktdev.streamit.service.api.content

import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.TestContentData
import no.iktdev.streamit.service.assertHttpOk
import no.iktdev.streamit.service.assertJson
import no.iktdev.streamit.service.model.shared.content.Catalog
import no.iktdev.streamit.service.simpleGet
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference

class CatalogControllerTest : TestBaseWithDatabase() {
    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @BeforeAll
    fun insertCatalogContent() {
        TestContentData.movie("Potetmonsteret", genre = "Test")
        TestContentData.movie("Gulrotspøkelset", genre = "Test")
        TestContentData.movie("Epleskurken", genre = "Test")
        TestContentData.movie("Tomattrøbbel", genre = "Test")
    }

    @Test
    fun `Catalog returns entries with titles stored separately`() {
        val response = restTemplate.simpleGet("/open/api/v1/catalog", object : ParameterizedTypeReference<List<Catalog>>() {})
        assertHttpOk(response)
        val entry = response.body.first { it.title == "Potetmonsteret" }
        assertJson("""{"title":"Potetmonsteret","collection":"Potetmonsteret","cover":"Potetmonsteret.jpg","type":"Movie"}""", entry)
    }

    @Test
    fun `Movie endpoint returns catalogs from the new movie relation`() {
        val response = restTemplate.simpleGet("/open/api/v1/catalog/movie", object : ParameterizedTypeReference<List<Catalog>>() {})
        assertHttpOk(response)
        assert(response.body.any { it.title == "Potetmonsteret" })
    }

    @Test
    fun `Genre endpoint filters through catalog genre relation`() {
        val genres = restTemplate.simpleGet("/open/api/v1/genre", object : ParameterizedTypeReference<List<no.iktdev.streamit.service.model.shared.content.Genre>>() {})
        assertHttpOk(genres)
        val response = restTemplate.simpleGet("/open/api/v1/catalog/genre?genreId=${genres.body.single { it.genre == "Test" }.id}", object : ParameterizedTypeReference<List<Catalog>>() {})
        assertHttpOk(response)
        assert(response.body.any { it.title == "Potetmonsteret" })
    }
}
