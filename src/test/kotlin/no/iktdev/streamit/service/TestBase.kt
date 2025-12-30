package no.iktdev.streamit.service

import io.mockk.every
import io.mockk.mockkObject
import no.iktdev.streamit.service.Env
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.File
import java.net.URI

@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ExtendWith(SpringExtension::class)
class TestBase {
    @LocalServerPort
    var port: Int = 0

    @Bean
    fun testRestTemplate(): TestRestTemplate {
        val baseUrl = URI("http://localhost:$port")
        return TestRestTemplate(RestTemplateBuilder().rootUri(baseUrl.toString()))
    }

    init {
        mockFolders()
    }


}