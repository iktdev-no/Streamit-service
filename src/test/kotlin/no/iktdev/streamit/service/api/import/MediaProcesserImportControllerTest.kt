package no.iktdev.streamit.service.api.import

import io.kotest.matchers.shouldBe
import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.db.tables.content.CatalogTable
import no.iktdev.streamit.service.db.tables.content.GenreTable
import no.iktdev.streamit.service.db.tables.content.MovieTable
import no.iktdev.streamit.service.db.tables.content.ProgressTable
import no.iktdev.streamit.service.db.tables.content.SerieTable
import no.iktdev.streamit.service.db.tables.content.SubtitleTable
import no.iktdev.streamit.service.db.tables.content.SummaryTable
import no.iktdev.streamit.service.dto.MediaProcesserImport
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

class MediaProcesserImportControllerTest : TestBaseWithDatabase() {
    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @AfterEach
    fun clearProgress() {
        transaction {
            clearTables()
        }
    }

    @Test
    fun verifyRequiresAuthOnSecure() {
        val payload = MediaProcesserImport(
            collection = "movies",
            metadata = MediaProcesserImport.MetadataImport(
                title = "The Matrix",
                genres = listOf("Sci-Fi", "Action"),
                cover = "matrix.jpg",
                summary = listOf(
                    MediaProcesserImport.MetadataImport.Summary(
                        "en",
                        "A hacker discovers reality is fake."
                    )
                ),
                mediaType = MediaProcesserImport.MediaType.Movie,
                source = "local"
            ),
            media = MediaProcesserImport.MediaImport(
                videoFile = "matrix.mkv",
                subtitles = listOf(MediaProcesserImport.MediaImport.Subtitle("matrix.en.srt", "en"))
            ),
            episodeInfo = null
        )

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val entity = HttpEntity(payload, headers)
        val response = restTemplate.postForEntity(
            "/secure/api/mediaprocesser/import",
            entity,
            Void::class.java
        )
        assertEquals(response.statusCode, HttpStatus.UNAUTHORIZED)

    }


    @Test
    fun movieImport() {
        val payload = MediaProcesserImport(
            collection = "movies",
            metadata = MediaProcesserImport.MetadataImport(
                title = "The Matrix",
                genres = listOf("Sci-Fi", "Action"),
                cover = "matrix.jpg",
                summary = listOf(
                    MediaProcesserImport.MetadataImport.Summary(
                        "en",
                        "A hacker discovers reality is fake."
                    )
                ),
                mediaType = MediaProcesserImport.MediaType.Movie,
                source = "local"
            ),
            media = MediaProcesserImport.MediaImport(
                videoFile = "matrix.mkv",
                subtitles = listOf(MediaProcesserImport.MediaImport.Subtitle("matrix.en.srt", "en"))
            ),
            episodeInfo = null
        )

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val entity = HttpEntity(payload, headers)
        val response = restTemplate.postForEntity(
            "/open/api/mediaprocesser/import",
            entity,
            Void::class.java
        )
        assertEquals(response.statusCode, HttpStatus.OK)
        transaction {
            // Movie inserted
            val movie = MovieTable.selectAll().single()
            assertThat(movie[MovieTable.video]).isEqualTo("matrix.mkv")

            // Genres inserted
            GenreTable.selectAll().count() shouldBe 2

            // Catalog inserted
            val catalog = CatalogTable.selectAll().single()
            catalog[CatalogTable.title] shouldBe "The Matrix"
            catalog[CatalogTable.collection] shouldBe "movies"

            // Summary inserted
            SummaryTable.selectAll().count() shouldBe 1

            // Subtitle inserted
            SubtitleTable.selectAll().count() shouldBe 1
        }

    }

    @Test
    fun serieImport_withEpisodeInfo() {
        val payload = MediaProcesserImport(
            collection = "series",
            metadata = MediaProcesserImport.MetadataImport(
                title = "Breaking Bad",
                genres = listOf("Drama"),
                cover = "bb.jpg",
                summary = listOf(
                    MediaProcesserImport.MetadataImport.Summary(
                        "en",
                        "Walter White cooks meth."
                    )
                ),
                mediaType = MediaProcesserImport.MediaType.Serie,
                source = "local"
            ),
            media = MediaProcesserImport.MediaImport(
                videoFile = "breakingbad.s01e01.mkv",
                subtitles = listOf(
                    MediaProcesserImport.MediaImport.Subtitle("bb.en.srt", "en")
                )
            ),
            episodeInfo = MediaProcesserImport.EpisodeInfo(
                episodeNumber = 1,
                seasonNumber = 1,
                episodeTitle = "Pilot"
            )
        )

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val entity = HttpEntity(payload, headers)
        val response = restTemplate.postForEntity(
            "/open/api/mediaprocesser/import",
            entity,
            Void::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        transaction {
            // Episode inserted
            val ep = SerieTable.selectAll().single()
            assertThat(ep[SerieTable.season]).isEqualTo(1)
            assertThat(ep[SerieTable.episode]).isEqualTo(1)
            assertThat(ep[SerieTable.title]).isEqualTo("Pilot")
            assertThat(ep[SerieTable.video]).isEqualTo("breakingbad.s01e01.mkv")

            // Genre inserted
            GenreTable.selectAll().count() shouldBe 1

            // Catalog inserted
            val catalog = CatalogTable.selectAll().single()
            catalog[CatalogTable.title] shouldBe "Breaking Bad"
            catalog[CatalogTable.collection] shouldBe "series"

            // Summary inserted
            SummaryTable.selectAll().count() shouldBe 1

            // Subtitle inserted
            SubtitleTable.selectAll().count() shouldBe 1
        }
    }

    @Test
    fun serieImport_withoutEpisodeTitle() {
        val payload = MediaProcesserImport(
            collection = "series",
            metadata = MediaProcesserImport.MetadataImport(
                title = "The Office",
                genres = listOf("Comedy"),
                cover = null,
                summary = emptyList(),
                mediaType = MediaProcesserImport.MediaType.Serie,
                source = "local"
            ),
            media = MediaProcesserImport.MediaImport(
                videoFile = "theoffice.s02e03.mkv",
                subtitles = emptyList()
            ),
            episodeInfo = MediaProcesserImport.EpisodeInfo(
                episodeNumber = 3,
                seasonNumber = 2,
                episodeTitle = null
            )
        )

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val entity = HttpEntity(payload, headers)
        val response = restTemplate.postForEntity(
            "/open/api/mediaprocesser/import",
            entity,
            Void::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        transaction {
            val ep = SerieTable.selectAll().single()
            assertThat(ep[SerieTable.season]).isEqualTo(2)
            assertThat(ep[SerieTable.episode]).isEqualTo(3)

            // episodeTitle kan være null eller tom avhengig av din implementasjon
            // så vi sjekker bare at den finnes i tabellen
            assertThat(ep[SerieTable.title]).isNull()

            // Catalog inserted
            CatalogTable.selectAll().count() shouldBe 1
        }
    }

    @Test
    fun serieImport_withMultipleSubtitles() {
        val payload = MediaProcesserImport(
            collection = "series",
            metadata = MediaProcesserImport.MetadataImport(
                title = "Game of Thrones",
                genres = listOf("Fantasy"),
                cover = "got.jpg",
                summary = emptyList(),
                mediaType = MediaProcesserImport.MediaType.Serie,
                source = "local"
            ),
            media = MediaProcesserImport.MediaImport(
                videoFile = "got.s01e02.mkv",
                subtitles = listOf(
                    MediaProcesserImport.MediaImport.Subtitle("got.en.srt", "en"),
                    MediaProcesserImport.MediaImport.Subtitle("got.no.srt", "no"),
                    MediaProcesserImport.MediaImport.Subtitle("got.es.srt", "es")
                )
            ),
            episodeInfo = MediaProcesserImport.EpisodeInfo(
                episodeNumber = 2,
                seasonNumber = 1,
                episodeTitle = "The Kingsroad"
            )
        )

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val entity = HttpEntity(payload, headers)
        val response = restTemplate.postForEntity(
            "/open/api/mediaprocesser/import",
            entity,
            Void::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        transaction {
            // Episode inserted
            SerieTable.selectAll().count() shouldBe 1

            // Subtitles inserted
            SubtitleTable.selectAll().count() shouldBe 3

            // Catalog inserted
            CatalogTable.selectAll().count() shouldBe 1
        }
    }

    @Test
    fun serieImport_episode2_whenEpisode1AlreadyExists() {

        // ---------- STEP 1: Import episode 1 ----------
        val ep1 = MediaProcesserImport(
            collection = "series",
            metadata = MediaProcesserImport.MetadataImport(
                title = "Breaking Bad",
                genres = listOf("Drama"),
                cover = "bb.jpg",
                summary = listOf(
                    MediaProcesserImport.MetadataImport.Summary(
                        "en",
                        "Walter White cooks meth."
                    )
                ),
                mediaType = MediaProcesserImport.MediaType.Serie,
                source = "local"
            ),
            media = MediaProcesserImport.MediaImport(
                videoFile = "breakingbad.s01e01.mkv",
                subtitles = listOf(
                    MediaProcesserImport.MediaImport.Subtitle("bb.en.srt", "en")
                )
            ),
            episodeInfo = MediaProcesserImport.EpisodeInfo(
                episodeNumber = 1,
                seasonNumber = 1,
                episodeTitle = "Pilot"
            )
        )

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        restTemplate.postForEntity(
            "/open/api/mediaprocesser/import",
            HttpEntity(ep1, headers),
            Void::class.java
        ).statusCode shouldBe HttpStatus.OK


        // ---------- STEP 2: Import episode 2 ----------
        val ep2 = MediaProcesserImport(
            collection = "series",
            metadata = MediaProcesserImport.MetadataImport(
                title = "Breaking Bad",
                genres = listOf("Drama"), // same genre
                cover = "bb.jpg",
                summary = listOf(
                    MediaProcesserImport.MetadataImport.Summary(
                        "en",
                        "Walter White continues cooking meth."
                    )
                ),
                mediaType = MediaProcesserImport.MediaType.Serie,
                source = "local"
            ),
            media = MediaProcesserImport.MediaImport(
                videoFile = "breakingbad.s01e02.mkv",
                subtitles = listOf(
                    MediaProcesserImport.MediaImport.Subtitle("bb.no.srt", "no")
                )
            ),
            episodeInfo = MediaProcesserImport.EpisodeInfo(
                episodeNumber = 2,
                seasonNumber = 1,
                episodeTitle = "Cat's in the Bag"
            )
        )

        restTemplate.postForEntity(
            "/open/api/mediaprocesser/import",
            HttpEntity(ep2, headers),
            Void::class.java
        ).statusCode shouldBe HttpStatus.OK


        // ---------- STEP 3: Verify DB state ----------
        transaction {

            // We should now have 2 episodes
            SerieTable.selectAll().count() shouldBe 2

            val episodes = SerieTable.selectAll().sortedBy { it[SerieTable.episode] }.toList()

            episodes[0][SerieTable.episode] shouldBe 1
            episodes[0][SerieTable.title] shouldBe "Pilot"
            episodes[0][SerieTable.video] shouldBe "breakingbad.s01e01.mkv"

            episodes[1][SerieTable.episode] shouldBe 2
            episodes[1][SerieTable.title] shouldBe "Cat's in the Bag"
            episodes[1][SerieTable.video] shouldBe "breakingbad.s01e02.mkv"


            // Catalog should NOT duplicate
            CatalogTable.selectAll().count() shouldBe 1

            val catalog = CatalogTable.selectAll().single()
            catalog[CatalogTable.title] shouldBe "Breaking Bad"
            catalog[CatalogTable.collection] shouldBe "series"


            // Genre should not duplicate
            GenreTable.selectAll().count() shouldBe 1


            // Summary should not duplicate (insertIgnore)
            SummaryTable.selectAll().count() shouldBe 1


            // Subtitles: 1 from ep1 + 1 from ep2
            SubtitleTable.selectAll().count() shouldBe 2
        }
    }

    @Test
    fun catalogCover_shouldNotBeOverwritten_whenAlreadyExists() {

        // ---------- STEP 1: Import with cover ----------
        val firstImport = MediaProcesserImport(
            collection = "movies",
            metadata = MediaProcesserImport.MetadataImport(
                title = "Inception",
                genres = listOf("Sci-Fi"),
                cover = "inception.jpg",
                summary = emptyList(),
                mediaType = MediaProcesserImport.MediaType.Movie,
                source = "local"
            ),
            media = MediaProcesserImport.MediaImport(
                videoFile = "inception.mkv",
                subtitles = emptyList()
            ),
            episodeInfo = null
        )

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        restTemplate.postForEntity(
            "/open/api/mediaprocesser/import",
            HttpEntity(firstImport, headers),
            Void::class.java
        ).statusCode shouldBe HttpStatus.OK


        // ---------- STEP 2: Import again WITHOUT cover ----------
        val secondImport = firstImport.copy(
            metadata = firstImport.metadata.copy(
                cover = "inception2.jpg" // intentionally missing
            )
        )

        restTemplate.postForEntity(
            "/open/api/mediaprocesser/import",
            HttpEntity(secondImport, headers),
            Void::class.java
        ).statusCode shouldBe HttpStatus.OK


        // ---------- STEP 3: Verify cover was NOT overwritten ----------
        transaction {
            val catalog = CatalogTable.selectAll().single()

            catalog[CatalogTable.cover] shouldBe "inception.jpg" // unchanged
        }
    }

    @Test
    fun catalogCover_shouldBeFilled_whenMissing() {

        // ---------- STEP 1: Import WITHOUT cover ----------
        val firstImport = MediaProcesserImport(
            collection = "movies",
            metadata = MediaProcesserImport.MetadataImport(
                title = "Interstellar",
                genres = listOf("Sci-Fi"),
                cover = null, // missing
                summary = emptyList(),
                mediaType = MediaProcesserImport.MediaType.Movie,
                source = "local"
            ),
            media = MediaProcesserImport.MediaImport(
                videoFile = "interstellar.mkv",
                subtitles = emptyList()
            ),
            episodeInfo = null
        )

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        restTemplate.postForEntity(
            "/open/api/mediaprocesser/import",
            HttpEntity(firstImport, headers),
            Void::class.java
        ).statusCode shouldBe HttpStatus.OK


        // ---------- STEP 2: Import again WITH cover ----------
        val secondImport = firstImport.copy(
            metadata = firstImport.metadata.copy(
                cover = "interstellar.jpg"
            )
        )

        restTemplate.postForEntity(
            "/open/api/mediaprocesser/import",
            HttpEntity(secondImport, headers),
            Void::class.java
        ).statusCode shouldBe HttpStatus.OK


        // ---------- STEP 3: Verify cover WAS updated ----------
        transaction {
            val catalog = CatalogTable.selectAll().single()

            catalog[CatalogTable.cover] shouldBe "interstellar.jpg" // updated
        }
    }




}