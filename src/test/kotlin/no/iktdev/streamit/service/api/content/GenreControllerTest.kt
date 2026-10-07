package no.iktdev.streamit.service.api.content

import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.TestContentData
import no.iktdev.streamit.service.assertHttpOk
import no.iktdev.streamit.service.assertJson
import no.iktdev.streamit.service.model.shared.content.Genre
import no.iktdev.streamit.service.simpleGet
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference

class GenreControllerTest : TestBaseWithDatabase() {
    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @BeforeAll
    fun insertCatalogContent() {
        TestContentData.movie("Test movie", genre = "Test")
    }

    @Test
    fun `Genres should return entries from the V2 genre table`() {
        val response = restTemplate.simpleGet("/open/api/v1/genre", object : ParameterizedTypeReference<List<Genre>>() {})
        assertHttpOk(response)
        assertJson("""[{"id":1,"genre":"Test"}]""", response.body)
    }
}
