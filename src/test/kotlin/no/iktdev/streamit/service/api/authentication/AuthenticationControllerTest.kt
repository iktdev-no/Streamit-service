package no.iktdev.streamit.service.api.authentication

import io.mockk.every
import io.mockk.mockkObject
import no.iktdev.streamit.service.TestBase
import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.shared.Authentication
import no.iktdev.streamit.shared.Env
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.io.File

class AuthenticationControllerTest: TestBaseWithDatabase() {
    @Autowired
    lateinit var restTemplate: TestRestTemplate


    // 1. Åpent endepunkt skal gi 200 OK uten autentisering
    @Test
    fun `open accessible endpoint should return OK`() {
        val headers = HttpHeaders().apply {
        }
        val request = HttpEntity<Void>(headers)

        val response = restTemplate.exchange(
            "/open/api/auth/accessible", // 👈 bare path
            HttpMethod.GET,
            request,
            Void::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    // 2. Sikkert endepunkt med gyldig JWT
    @Test
    fun `secure accessible endpoint should return OK with valid JWT in header`() {
        val request = HttpEntity<Void>(HttpHeaders().apply {
            setBearerAuth(validToken)
        })

        val response = restTemplate.exchange(
            "/secure/api/auth/accessible", // 👈 bare path
            HttpMethod.GET,
            request,
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `secure accessible endpoint should return OK with valid JWT in url`() {
        val request = HttpEntity<Void>(HttpHeaders())

        val response = restTemplate.exchange(
            "/secure/api/auth/accessible?token=$validToken", // 👈 bare path
            HttpMethod.GET,
            request,
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    // 3. Sikkert endepunkt uten autentisering -> 401
    @Test
    fun `secure validate endpoint should return 401 without JWT`() {
        val headers = HttpHeaders().apply {
        }
        val request = HttpEntity<Void>(headers)

        val response = restTemplate.exchange(
            "/secure/api/auth/accessible", // 👈 bare path
            HttpMethod.GET,
            request,
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `secure accessible endpoint should return Unauthorized Request with invalid JWT in header`() {
        val request = HttpEntity<Void>(HttpHeaders().apply {
            setBearerAuth("potetmos")
        })

        val response = restTemplate.exchange(
            "/secure/api/auth/accessible", // 👈 bare path
            HttpMethod.GET,
            request,
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `secure accessible endpoint should return Unauthorized Request with invalid token passed in url`() {
        val request = HttpEntity<Void>(HttpHeaders())

        val response = restTemplate.exchange(
            "/secure/api/auth/accessible?token=potetmos", // 👈 bare path
            HttpMethod.GET,
            request,
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

}