package no.iktdev.streamit.service.api.content

import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.TestContentData
import no.iktdev.streamit.service.model.internal.user.UserData
import no.iktdev.streamit.service.model.shared.content.Progress
import no.iktdev.streamit.service.stores.user.IUserStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class ViewProgressControllerTest : TestBaseWithDatabase() {
    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var userStore: IUserStore

    private lateinit var userId: UUID
    private var videoId: Long = 0

    @BeforeEach
    fun createUserAndVideo() {
        userId = UUID.fromString(userStore.insert(UserData("Progress user-${UUID.randomUUID()}", "Default-0.png")).uid)
        videoId = requireNotNull(TestContentData.movie("Progress movie").videoId)
    }

    @Test
    fun `progress upsert replaces the existing user and video entry`() {
        val firstPlayed = Instant.now().truncatedTo(ChronoUnit.MICROS)
        val first = Progress(played = firstPlayed, position = 120L)
        val firstResponse = restTemplate.postForEntity(
            "/open/api/v1/video/progress/$userId/video/$videoId",
            HttpEntity(first, HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }),
            Boolean::class.java
        )
        assertEquals(true, firstResponse.body)

        val updated = Progress(played = firstPlayed.plusSeconds(10), position = 300L)
        val updateResponse = restTemplate.postForEntity(
            "/open/api/v1/video/progress/$userId/video/$videoId",
            HttpEntity(updated, HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }),
            Boolean::class.java
        )
        assertEquals(true, updateResponse.body)

        val byVideo = restTemplate.getForObject(
            "/open/api/v1/video/progress/$userId/video/$videoId",
            Progress::class.java
        )
        assertEquals(updated, byVideo)

        val all = restTemplate.getForObject(
            "/open/api/v1/video/progress/$userId",
            Array<Progress>::class.java
        )
        assertEquals(listOf(updated), all?.toList())

        val after = restTemplate.getForObject(
            "/open/api/v1/video/progress/$userId/after/${firstPlayed}",
            Array<Progress>::class.java
        )
        assertEquals(listOf(updated), after?.toList())
    }
}
