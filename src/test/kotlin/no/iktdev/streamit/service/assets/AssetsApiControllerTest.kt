package no.iktdev.streamit.service.assets

import no.iktdev.streamit.service.TestBase
import no.iktdev.streamit.service.mockFolders
import no.iktdev.streamit.service.Env
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.io.File

class AssetsApiControllerTest: TestBase() {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            mockFolders()


            // Initialize the TestRestTemplate here if needed
            val resource = ClassPathResource("assets/Default-3.png")
            val targetFile = File(Env.getAssetsFolder(), "Default-3.png")
            resource.inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    @Test
    fun `Open assets`() {
        val headers = HttpHeaders()
        val request = HttpEntity<Void>(headers)

        val response = restTemplate.exchange(
            "/open/assets/profile-image/Default-3.png",
            HttpMethod.GET,
            request,
            Void::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `Secure assets`() {
        val headers = HttpHeaders()
        val request = HttpEntity<Void>(headers)

        val response = restTemplate.exchange(
            "/secure/assets/profile-image/Default-3.png",
            HttpMethod.GET,
            request,
            Void::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }



}