package no.iktdev.streamit.service.api.import

import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.db.tables.content.v2.*
import no.iktdev.streamit.service.model.shared.ImportReference
import no.iktdev.streamit.service.model.shared.content.ContentType
import no.iktdev.streamit.service.model.shared.contentImport.*
import no.iktdev.streamit.service.services.ImportContentService
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

class MediaImportControllerTest : TestBaseWithDatabase() {
    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var importContentService: ImportContentService

    @AfterEach
    fun clearDatabaseTables() {
        transaction { clearTables() }
    }

    @Test
    fun `movie import writes the new normalized relations`() {
        val payload = MediaImportV2(
            reference = ImportReference(store = "movies"),
            metadata = CatalogMetadata(
                title = "The Matrix",
                genres = listOf("Sci-Fi", "Action"),
                cover = "matrix.jpg",
                type = ContentType.Movie,
                summaries = listOf(Summary("eng", "A hacker discovers reality is fake."))
            ),
            media = Media(
                content = MediaContent.Movie("matrix.mkv"),
                subtitles = listOf(SubtitleImport("matrix.en.srt", "eng", "srt"))
            )
        )

        assertEquals(true, importContentService.import(payload))

        transaction {
            val catalog = CatalogTableV2.selectAll().single()
            val catalogId = catalog[CatalogTableV2.id].value
            val title = CatalogTitleTableV2.selectAll().single()
            val video = VideoTableV2.selectAll().single()
            val movie = MovieTableV2.selectAll().single()

            assertEquals("movies", catalog[CatalogTableV2.store])
            assertEquals("matrix.jpg", catalog[CatalogTableV2.cover])
            assertEquals("The Matrix", title[CatalogTitleTableV2.title])
            assertEquals(catalogId, title[CatalogTitleTableV2.catalogId].value)
            assertEquals("matrix.mkv", video[VideoTableV2.file])
            assertEquals(catalogId, movie[MovieTableV2.catalogId].value)
            assertEquals(video[VideoTableV2.id].value, movie[MovieTableV2.videoId].value)
            assertEquals(2, GenreTableV2.selectAll().count().toInt())
            assertEquals("A hacker discovers reality is fake.", SummaryTableV2.selectAll().single()[SummaryTableV2.description])
            assertEquals("matrix.en.srt", SubtitleTableV2.selectAll().single()[SubtitleTableV2.file])
        }
    }

    @Test
    fun `episode import uses catalog id and stores episode against video`() {
        val createCatalog = MediaImportV2(
            reference = ImportReference(store = "series"),
            metadata = CatalogMetadata(title = "Breaking Bad", type = ContentType.Serie)
        )
        assertEquals(true, importContentService.import(createCatalog))
        val catalogId = transaction { CatalogTableV2.selectAll().single()[CatalogTableV2.id].value }

        val importEpisode = MediaImportV2(
            reference = ImportReference(catalogId = catalogId, store = "series"),
            media = Media(
                content = MediaContent.Episode("breakingbad.s01e01.mkv", season = 1, episode = 1, title = "Pilot"),
                subtitles = listOf(SubtitleImport("bb.en.srt", "eng", "srt"))
            )
        )
        assertEquals(true, importContentService.import(importEpisode))

        transaction {
            val episode = SerieTableV2.selectAll().single()
            assertEquals(catalogId, episode[SerieTableV2.catalogId].value)
            assertEquals(1, episode[SerieTableV2.season])
            assertEquals(1, episode[SerieTableV2.episode])
            assertEquals("Pilot", episode[SerieTableV2.title])
            assertEquals("breakingbad.s01e01.mkv", VideoTableV2.selectAll().single()[VideoTableV2.file])
        }
    }

    @Test
    fun `controller accepts metadata imports using the V2 payload`() {
        val payload = MediaImportV2(
            reference = ImportReference(store = "movies"),
            metadata = CatalogMetadata(title = "The Matrix", type = ContentType.Movie)
        )

        val response = postImport(payload)

        assertEquals(HttpStatus.OK, response.statusCode)
        transaction {
            assertEquals("The Matrix", CatalogTitleTableV2.selectAll().single()[CatalogTitleTableV2.title])
        }
    }

    @Test
    fun `secure import endpoint requires authentication`() {
        val payload = MediaImportV2(
            reference = ImportReference(store = "movies"),
            metadata = CatalogMetadata(title = "The Matrix", type = ContentType.Movie)
        )
        val response = postImport(payload, secure = true)
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    private fun postImport(payload: MediaImportV2, secure: Boolean = false) =
        restTemplate.postForEntity(
            "/${if (secure) "secure" else "open"}/api/media/import/import",
            HttpEntity(payload, HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }),
            Void::class.java
        )
}
