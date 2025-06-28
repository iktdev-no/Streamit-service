package no.iktdev.streamit.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockkObject
import no.iktdev.streamit.shared.Authentication.Companion.algorithm
import no.iktdev.streamit.shared.Authentication.Companion.issuer
import no.iktdev.streamit.shared.Env
import no.iktdev.streamit.shared.classes.Catalog
import org.assertj.core.api.Assertions.assertThat
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.io.File
import java.time.Instant
import java.util.Date

val objectMapper = ObjectMapper()


val defaultHeaderRequest = HttpEntity<Void>(HttpHeaders())

fun <T> TestRestTemplate.simpleGet(path: String, response: ParameterizedTypeReference<T>): ResponseEntity<T> {
    return this.exchange(
        path,
        HttpMethod.GET,
        defaultHeaderRequest,
        response
    )
}

fun assertHttpOk(response: ResponseEntity<*>) {
    assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
}

fun assertJson(expected: String, actual: Any?) {
    val asJson = objectMapper.writeValueAsString(actual)
    JSONAssert.assertEquals(expected, asJson, JSONCompareMode.LENIENT)
}

fun <T> T.asList(): List<T> {
    return listOf(this)
}

fun generateInvalidJwt(): String {
    val builder = JWT.create()
        .withIssuer(issuer)
        .withIssuedAt(Date.from(Instant.now()))
        .withSubject("Authorization for A.O.I.")

    val token = builder.sign(Algorithm.HMAC256("Unauthorized"))
    return token
}

fun mockFolders() {
    mockkObject(Env)
    every { Env.getConfigFolder() } returns File(System.getProperty("java.io.tmpdir"))
    every { Env.getAvahiServiceFolder() } returns File(System.getProperty("java.io.tmpdir"))
    every { Env.getAssetsFolder() } returns File(System.getProperty("java.io.tmpdir"))
}