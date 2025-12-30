package no.iktdev.streamit.service

import no.iktdev.streamit.service.dto.Heartbeat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class MainControllerTest: TestBase() {
    @Autowired
    lateinit var restTemplate: TestRestTemplate

    // 1. Åpent endepunkt skal gi 200 OK uten autentisering
    @Test
    fun `open index OK`() {
        val headers = HttpHeaders().apply {
        }
        val request = HttpEntity<Void>(headers)

        val response = restTemplate.exchange(
            "/open/api/", // 👈 bare path
            HttpMethod.GET,
            request,
            String::class.java
        )

        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun `open heartbeat OK`() {
        val headers = HttpHeaders().apply {
        }
        val request = HttpEntity<Void>(headers)

        val response = restTemplate.exchange(
            "/open/api/heartbeat", // 👈 bare path
            HttpMethod.GET,
            request,
            Heartbeat::class.java
        )

        assert(response.statusCode == HttpStatus.OK)
        assert(response.body.status)
    }
}