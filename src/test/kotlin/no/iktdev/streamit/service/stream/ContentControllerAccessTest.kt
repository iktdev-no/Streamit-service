package no.iktdev.streamit.service.stream

import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.generateInvalidJwt
import no.iktdev.streamit.shared.Authentication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class ContentControllerAccessTest: TestBaseWithDatabase() {
    lateinit var auth: Authentication
    @Autowired
    lateinit var restTemplate: TestRestTemplate


    lateinit var jwt: String
    @BeforeAll
    fun createToken() {
        // For å kunne bruke JWT i testene
        auth = Authentication()
        jwt = auth.createJwt()
    }

    @Test
    fun `open stream media video endpoint returns 404 when file does not exist`() {
        val headers = HttpHeaders()
        val request = HttpEntity<Void>(headers)

        val response = restTemplate.exchange(
            "/open/stream/media/video/testCollection/testVideo.mp4",
            HttpMethod.GET,
            request,
            Void::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // 2. Sikkert endepunkt med gyldig JWT
    @Test
    fun `secure accessible endpoint should return OK with valid JWT`() {
        val request = HttpEntity<Void>(HttpHeaders().apply {
            setBearerAuth(jwt)
        })

        val response = restTemplate.exchange(
            "/secure/stream/media/video/testCollection/testVideo.mp4",
            HttpMethod.GET,
            request,
            Void::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // 2. Sikkert endepunkt med gyldig JWT
    @Test
    fun `secure accessible endpoint should return Unauthorized with invalid JWT`() {
        val request = HttpEntity<Void>(HttpHeaders().apply {
            setBearerAuth(generateInvalidJwt())
        })

        val response = restTemplate.exchange(
            "/secure/stream/media/video/testCollection/testVideo.mp4",
            HttpMethod.GET,
            request,
            Void::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

}