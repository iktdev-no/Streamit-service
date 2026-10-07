package no.iktdev.streamit.service.api.content

import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.TestContentData
import no.iktdev.streamit.service.model.internal.user.UserData
import no.iktdev.streamit.service.model.shared.content.Progress
import no.iktdev.streamit.service.stores.user.IUserStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
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
    fun `progress controller returns empty results when the user has no progress`() {
        val byVideo = restTemplate.getForObject(
            "/open/api/v1/video/progress/$userId/video/$videoId",
            Progress::class.java
        )
        assertNull(byVideo)

        val all = restTemplate.getForObject(
            "/open/api/v1/video/progress/$userId",
            Array<Progress>::class.java
        )
        assertEquals(emptyList<Progress>(), all?.toList())
    }
}
