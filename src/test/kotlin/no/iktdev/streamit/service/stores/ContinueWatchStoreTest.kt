package no.iktdev.streamit.service.stores

import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.db.tables.content.v2.CatalogTableV2
import no.iktdev.streamit.service.db.tables.content.v2.CatalogTitleTableV2
import no.iktdev.streamit.service.db.tables.content.v2.ContinueWatchTableV2
import no.iktdev.streamit.service.db.tables.content.v2.MovieTableV2
import no.iktdev.streamit.service.db.tables.content.v2.SerieTableV2
import no.iktdev.streamit.service.db.tables.content.v2.VideoTableV2
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.model.internal.user.UserData
import no.iktdev.streamit.service.model.shared.ContentType
import no.iktdev.streamit.service.model.shared.consumer.content.Progress
import no.iktdev.streamit.service.stores.continuewatch.IContinueWatchStore
import no.iktdev.streamit.service.stores.progress.IProgressStore
import no.iktdev.streamit.service.stores.user.IUserStore
import org.jetbrains.exposed.sql.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpEntity
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class ContinueWatchStoreTest : TestBaseWithDatabase() {
    @Autowired
    lateinit var continueWatchStore: IContinueWatchStore

    @Autowired
    lateinit var progressStore: IProgressStore

    @Autowired
    lateinit var userStore: IUserStore

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    private lateinit var userId: UUID

    @BeforeEach
    fun setUp() {
        withTransaction { clearTables() }
        userId = UUID.fromString(
            userStore.insert(UserData("Continue user ${UUID.randomUUID()}", "Default-0.png")).uid
        )
    }

    @Test
    fun `progress adds a movie to continue watching and returns its content`() {
        val catalogId = createMovie("Continue movie", duration = 2_700_000L)
        val videoId = videoFor(catalogId)
        val played = Instant.parse("2026-01-01T12:00:00Z")
        withTransaction {
            CatalogTitleTableV2.insert {
                it[CatalogTitleTableV2.catalogId] = catalogId
                it[CatalogTitleTableV2.title] = "Later preferred title"
                it[CatalogTitleTableV2.language] = "nob"
                it[CatalogTitleTableV2.preferred] = true
            }
        }

        assertTrue(progressStore.upsert(userId, videoId, Progress(played, 450_000L)))

        val item = continueWatchStore.getForUser(userId).single()
        assertEquals(catalogId, item.catalogId)
        assertEquals("Continue movie", item.title)
        assertEquals(ContentType.Movie, item.type)
        assertEquals("Continue movie.jpg", item.cover)
        assertEquals(videoId, item.video.id)
        assertEquals("Continue movie.mp4", item.video.videoFileName)
        assertEquals(2_700_000L, item.video.duration)
        assertEquals(Progress(played, 450_000L), item.progress)
        assertEquals(null, item.episode)
    }

    @Test
    fun `completing a movie removes it from continue watching`() {
        val catalogId = createMovie("Finished movie", duration = 2_700_000L)
        val videoId = videoFor(catalogId)

        progressStore.upsert(userId, videoId, Progress(Instant.parse("2026-01-01T12:00:00Z"), 1_000_000L))
        assertEquals(1, continueWatchStore.getForUser(userId).size)

        progressStore.upsert(userId, videoId, Progress(Instant.parse("2026-01-01T12:10:00Z"), 2_410_000L))

        assertTrue(continueWatchStore.getForUser(userId).isEmpty())
    }

    @Test
    fun `finishing a series episode points to the next episode and removes the final episode`() {
        val episodes = createSeries(
            "Continue series",
            listOf(
                EpisodeFixture(season = 1, number = 1, title = "First", duration = 1_800_000L),
                EpisodeFixture(season = 1, number = 2, title = "Second", duration = 1_800_000L),
                EpisodeFixture(season = 2, number = 1, title = "Third", duration = 1_800_000L)
            )
        )

        progressStore.upsert(
            userId,
            episodes[0].videoId,
            Progress(Instant.parse("2026-01-01T12:00:00Z"), 1_510_000L)
        )
        val next = continueWatchStore.getForUser(userId).single()
        assertEquals(episodes[0].catalogId, next.catalogId)
        assertEquals(episodes[1].videoId, next.video.id)
        assertEquals(1, next.episode?.season)
        assertEquals(2, next.episode?.number)
        assertEquals("Second", next.episode?.title)
        assertEquals(null, next.progress)

        progressStore.upsert(
            userId,
            episodes[1].videoId,
            Progress(Instant.parse("2026-01-01T12:30:00Z"), 1_510_000L)
        )
        val last = continueWatchStore.getForUser(userId).single()
        assertEquals(episodes[2].videoId, last.video.id)
        assertEquals(2, last.episode?.season)
        assertEquals(1, last.episode?.number)

        progressStore.upsert(
            userId,
            episodes[2].videoId,
            Progress(Instant.parse("2026-01-01T13:00:00Z"), 1_510_000L)
        )
        assertTrue(continueWatchStore.getForUser(userId).isEmpty())
    }

    @Test
    fun `hide excludes an item without deleting its continue watch row`() {
        val catalogId = createMovie("Hidden movie", duration = 2_700_000L)
        val videoId = videoFor(catalogId)
        progressStore.upsert(userId, videoId, Progress(Instant.parse("2026-01-01T12:00:00Z"), 500_000L))

        assertTrue(continueWatchStore.hide(userId, catalogId))
        assertTrue(continueWatchStore.getForUser(userId).isEmpty())
        assertFalse(continueWatchStore.hide(userId, Long.MAX_VALUE))

        val rowStillExists = withTransaction {
            ContinueWatchTableV2.selectAll()
                .where { ContinueWatchTableV2.catalogId eq catalogId }
                .singleOrNull()
                ?.get(ContinueWatchTableV2.hide)
        }.getOrNull()
        assertEquals(true, rowStillExists)
    }

    @Test
    fun `delete continue endpoint hides the selected catalog entry`() {
        val catalogId = createMovie("Endpoint hidden movie", duration = 2_700_000L)
        val videoId = videoFor(catalogId)
        progressStore.upsert(userId, videoId, Progress(Instant.parse("2026-01-01T12:00:00Z"), 500_000L))

        val response = restTemplate.exchange(
            "/open/api/v1/catalog/continue/$userId/$catalogId",
            HttpMethod.DELETE,
            HttpEntity.EMPTY,
            Void::class.java
        )

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        assertTrue(continueWatchStore.getForUser(userId).isEmpty())
    }

    @Test
    fun `unknown duration does not mark content as finished`() {
        val catalogId = createMovie("Unknown duration", duration = null)
        val videoId = videoFor(catalogId)

        progressStore.upsert(userId, videoId, Progress(Instant.parse("2026-01-01T12:00:00Z"), 99_000_000L))

        assertEquals(videoId, continueWatchStore.getForUser(userId).single().video.id)
    }

    private fun createMovie(title: String, duration: Long?): Long = withTransaction {
        val catalogId = CatalogTableV2.insertAndGetId {
            it[CatalogTableV2.store] = title
            it[CatalogTableV2.type] = ContentType.Movie
            it[CatalogTableV2.cover] = "$title.jpg"
        }.value
        CatalogTitleTableV2.insert {
            it[CatalogTitleTableV2.catalogId] = catalogId
            it[CatalogTitleTableV2.title] = title
            it[CatalogTitleTableV2.language] = "eng"
            it[CatalogTitleTableV2.preferred] = true
        }
        val videoId = VideoTableV2.insertAndGetId {
            it[VideoTableV2.file] = "$title.mp4"
            it[VideoTableV2.duration] = duration
        }.value
        MovieTableV2.insert {
            it[MovieTableV2.catalogId] = catalogId
            it[MovieTableV2.videoId] = videoId
        }
        catalogId
    }.getOrThrow()

    private fun videoFor(catalogId: Long): Long = withTransaction {
        MovieTableV2
            .selectAll()
            .where { MovieTableV2.catalogId eq catalogId }
            .single()[MovieTableV2.videoId]
            .value
    }.getOrThrow()

    private fun createSeries(title: String, episodeFixtures: List<EpisodeFixture>): List<SeriesEpisode> =
        withTransaction {
            val catalogId = CatalogTableV2.insertAndGetId {
                it[CatalogTableV2.store] = title
                it[CatalogTableV2.type] = ContentType.Serie
                it[CatalogTableV2.cover] = "$title.jpg"
            }.value
            CatalogTitleTableV2.insert {
                it[CatalogTitleTableV2.catalogId] = catalogId
                it[CatalogTitleTableV2.title] = title
                it[CatalogTitleTableV2.language] = "eng"
                it[CatalogTitleTableV2.preferred] = true
            }
            episodeFixtures.map { fixture ->
                val videoId = VideoTableV2.insertAndGetId {
                    it[VideoTableV2.file] = "$title-S${fixture.season}E${fixture.number}.mp4"
                    it[VideoTableV2.duration] = fixture.duration
                }.value
                SerieTableV2.insert {
                    it[SerieTableV2.catalogId] = catalogId
                    it[SerieTableV2.videoId] = videoId
                    it[SerieTableV2.season] = fixture.season
                    it[SerieTableV2.episode] = fixture.number
                    it[SerieTableV2.title] = fixture.title
                    it[SerieTableV2.addedAt] = LocalDateTime.now()
                }
                SeriesEpisode(catalogId, videoId)
            }
        }.getOrThrow()

    private data class EpisodeFixture(
        val season: Int,
        val number: Int,
        val title: String,
        val duration: Long
    )

    private data class SeriesEpisode(val catalogId: Long, val videoId: Long)
}
