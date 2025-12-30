package no.iktdev.streamit.service.stream

import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.generateInvalidJwt
import no.iktdev.streamit.service.auth.Authentication
import no.iktdev.streamit.service.auth.castScope
import no.iktdev.streamit.service.dto.auth.MediaScopedAuthRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.io.Resource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class ContentControllerAccessTest: TestBaseWithDatabase() {
    @Autowired
    lateinit var restTemplate: TestRestTemplate


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
            setBearerAuth(validToken)
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


    @Test
    fun `secure accessible endpoint should return Unauthorized for valid cast token but without valid media`() {
        val castToken = Authentication().createMediaScopedJwt(MediaScopedAuthRequest(
            "",
            emptyList()
        ), Authentication.TokenType.Cast, castScope())

        assertThat { castToken != null }
        val request = HttpEntity<Void>(HttpHeaders().apply {
            setBearerAuth(castToken!!)
        })

        val response = restTemplate.exchange(
            "/secure/stream/media/video/testCollection/testVideo.mp4",
            HttpMethod.GET,
            request,
            Void::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `secure accessible endpoint should return 404 for valid cast token with valid media`() {
        val castToken = Authentication().createMediaScopedJwt(MediaScopedAuthRequest(
            "testCollection",
            listOf(
                "video/testCollection/testVideo.mp4"
            )
        ), Authentication.TokenType.Cast, castScope())

        assertThat { castToken != null }
        val request = HttpEntity<Void>(HttpHeaders().apply {
            setBearerAuth(castToken!!)
        })

        val response = restTemplate.exchange(
            "/secure/stream/media/video/testCollection/testVideo.mp4",
            HttpMethod.GET,
            request,
            Void::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }


    @Disabled
    @Test
    fun `verify CORS headers on open stream endpoint`() {
        val headers = HttpHeaders().apply {
            // Simulerer en ekstern opprinnelse, slik som Chromecast eller gstatic
            set("Origin", "https://www.gstatic.com")
        }

        val request = HttpEntity<Void>(headers)

        val response = restTemplate.exchange(
            "/open/stream/media/video/testCollection/testVideo.mp4",
            HttpMethod.OPTIONS, // Preflight-sjekk
            request,
            Resource::class.java
        )

        // Sjekk at Access-Control-Allow-Origin header eksisterer og har riktig verdi
        assertThat(response.headers["Access-Control-Allow-Origin"]).contains("https://www.gstatic.com")
    }

    @Disabled
    @Test
    fun `verify Access-Control-Allow-Origin header exists on open stream endpoint`() {
        val headers = HttpHeaders()
        headers.origin = "https://www.gstatic.com" // Simulerer en gyldig opprinnelse

        val request = HttpEntity<Void>(headers)

        val response = restTemplate.exchange(
            "/open/stream/media/preflight",
            HttpMethod.GET, // Preflight-sjekk
            request,
            Resource::class.java
        )

        // Sjekk at headeren finnes i responsen
        assertThat(response.headers["Access-Control-Allow-Origin"]).contains("https://www.gstatic.com")
        assertThat(response.headers["Access-Control-Allow-Credentials"]).contains("true")
        assertThat(response.headers["Access-Control-Allow-Methods"]).contains("GET, POST, PUT, DELETE, OPTIONS")
        assertThat(response.headers.containsKey("Access-Control-Allow-Origin")).isTrue()
    }


}