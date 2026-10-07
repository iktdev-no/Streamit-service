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

class SearchControllerTest : TestBaseWithDatabase() {
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
    fun `Search uses V2 titles`() {
        val response = restTemplate.simpleGet("/open/api/v1/search/Potet", object : ParameterizedTypeReference<List<Catalog>>() {})
        assertHttpOk(response)
        assertJson("""[{"id":1,"title":"Potetmonsteret","cover":"Potetmonsteret.jpg","type":"Movie","collection":"Potetmonsteret"}]""", response.body)
    }
}
