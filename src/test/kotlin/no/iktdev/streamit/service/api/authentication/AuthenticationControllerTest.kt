package no.iktdev.streamit.service.api.authentication

import no.iktdev.streamit.service.TestBaseWithDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class AuthenticationControllerTest: TestBaseWithDatabase() {
    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun `OpenAPI yaml endpoint is available without device authentication`() {
        val response = restTemplate.getForEntity("/v3/api-docs.yaml", String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("openapi:")
    }

    @Test
    fun `Importing OpenAPI contract marks CollectionReference fields as required`() {
        val response = restTemplate.getForEntity("/v3/api-docs/importing", String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val spec = com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body)
        val required = spec["components"]["schemas"]["CollectionReference"]["required"]
            .map { it.asText() }

        assertThat(required).containsExactly("catalogId", "store")
    }

    @Test
    fun `Grouped OpenAPI yaml endpoints are available without device authentication`() {
        listOf("consumer", "importing").forEach { group ->
            val response = restTemplate.getForEntity("/v3/api-docs.yaml/$group", String::class.java)

            assertThat(response.statusCode)
                .withFailMessage("OpenAPI YAML export for '$group' failed with ${response.statusCode}: ${response.body}")
                .isEqualTo(HttpStatus.OK)
            assertThat(response.body).contains("openapi:")
        }
    }

    @Test
    fun `Swagger UI is available without device authentication`() {
        val indexResponse = restTemplate.getForEntity("/swagger", String::class.java)
        val openResponse = restTemplate.getForEntity("/open/swagger-ui.html", String::class.java)
        val secureResponse = restTemplate.getForEntity("/secure/swagger-ui.html", String::class.java)

        assertThat(indexResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(indexResponse.body)
            .contains("Open route", "/v3/api-docs/open", "Secure route", "/v3/api-docs/secure")
            .doesNotContain("Consumer contract", "Importing contract")
        assertThat(openResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(openResponse.body).contains("/v3/api-docs/open")
        assertThat(secureResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(secureResponse.body).contains("/v3/api-docs/secure")
    }

    @Test
    fun `Open and secure OpenAPI groups use matching server prefixes`() {
        val openSpec = restTemplate.getForEntity("/v3/api-docs/open", String::class.java)
        val secureSpec = restTemplate.getForEntity("/v3/api-docs/secure", String::class.java)

        assertThat(openSpec.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(openSpec.body).contains("\"url\":\"/open\"")
        assertThat(openSpec.body).contains("media/importer/import", "metadata/catalog/search")
        assertThat(secureSpec.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(secureSpec.body).contains("\"url\":\"/secure\"")
        assertThat(secureSpec.body).contains("media/importer/import", "metadata/catalog/search")
    }


    // 2. Sikkert endepunkt med gyldig JWT
    @Test
    fun `secure accessible endpoint should return OK with valid JWT in header`() {
        val request = HttpEntity<Void>(HttpHeaders().apply {
            setBearerAuth(validToken)
        })

        val response = restTemplate.exchange(
            "/secure/api/v1/auth/validate", // 👈 bare path
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
            "/secure/api/v1/auth/validate?token=$validToken", // 👈 bare path
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
            "/secure/api/v1/auth/validate", // 👈 bare path
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
            "/secure/api/v1/auth/validate", // 👈 bare path
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
            "/secure/api/v1/auth/validate?token=potetmos", // 👈 bare path
            HttpMethod.GET,
            request,
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

}
