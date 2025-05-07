package no.iktdev.streamit.service.api.authentication

import no.iktdev.streamit.service.TestBase
import no.iktdev.streamit.shared.Authentication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class AuthenticationControllerTest: TestBase() {
    val auth = Authentication()
    @Autowired
    lateinit var restTemplate: TestRestTemplate

    // 1. Åpent endepunkt skal gi 200 OK uten autentisering
    @Test
    fun `open accessible endpoint should return OK`() {
        val headers = HttpHeaders().apply {
        }
        val request = HttpEntity<Void>(headers)

        val response = restTemplate.exchange(
            "/api/open/auth/accessible", // 👈 bare path
            HttpMethod.GET,
            request,
            Void::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    // 2. Sikkert endepunkt med gyldig JWT
    @Test
    fun `secure accessible endpoint should return OK with valid JWT`() {
        val jwt = auth.createJwt()
        val request = HttpEntity<Void>(HttpHeaders().apply {
            setBearerAuth(jwt.token)
        })

        val response = restTemplate.exchange(
            "/api/secure/auth/accessible", // 👈 bare path
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
            "/api/secure/auth/accessible", // 👈 bare path
            HttpMethod.GET,
            request,
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `secure accessible endpoint should return BAD Request with invalid JWT`() {
        val jwt = auth.createJwt()
        val request = HttpEntity<Void>(HttpHeaders().apply {
            setBearerAuth("potetmos")
        })

        val response = restTemplate.exchange(
            "/api/secure/auth/accessible", // 👈 bare path
            HttpMethod.GET,
            request,
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

}