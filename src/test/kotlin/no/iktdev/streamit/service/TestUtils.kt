package no.iktdev.streamit.service

import com.fasterxml.jackson.databind.ObjectMapper
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