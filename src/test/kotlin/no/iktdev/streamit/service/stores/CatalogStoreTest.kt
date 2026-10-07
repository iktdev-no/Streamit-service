package no.iktdev.streamit.service.stores

import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.TestContentData
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.model.shared.content.ContentType
import no.iktdev.streamit.service.stores.catalog.ICatalogStore
import no.iktdev.streamit.service.stores.subtitle.ISubtitleStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CatalogStoreTest : TestBaseWithDatabase() {
    @Autowired
    lateinit var catalogStore: ICatalogStore

    @Autowired
    lateinit var subtitleStore: ISubtitleStore

    @BeforeEach
    fun clearExistingRows() {
        withTransaction { clearTables() }
    }

    @Test
    fun `getMovieById joins catalog title video genres and subtitles`() {
        val fixture = TestContentData.movie("Test movie", store = "movies", videoFile = "test.mkv", genre = "Drama")
        val videoId = requireNotNull(fixture.videoId)
        subtitleStore.insert(videoId, "eng", "srt", "test.en.srt")

        val movie = catalogStore.getMovieById(fixture.catalogId)

        assertNotNull(movie)
        assertEquals("Test movie", movie?.title)
        assertEquals("movies", movie?.collection)
        assertEquals("test.mkv", movie?.video?.videoFileName)
        assertEquals(listOf("Drama"), movie?.genres?.map { it.genre })
        assertEquals(listOf("test.en.srt"), movie?.subtitles?.map { it.file })
    }

    @Test
    fun `getSerieById returns episode relations`() {
        val fixture = TestContentData.episode(
            title = "Test series",
            store = "series",
            videoFile = "s01e01.mkv",
            season = 1,
            episode = 1,
            genre = "Comedy"
        )

        val serie = catalogStore.getSerieById(fixture.catalogId)

        assertNotNull(serie)
        assertEquals(ContentType.Serie, serie?.type)
        assertEquals(1, serie?.episodes?.single()?.season)
        assertEquals(1, serie?.episodes?.single()?.episode)
        assertEquals("s01e01.mkv", serie?.episodes?.single()?.video?.videoFileName)
    }

    @Test
    fun `search filters movies and series by their separate title relation`() {
        TestContentData.movie("Searchable movie", genre = "Drama")
        TestContentData.episode("Searchable series", "series", "series.mkv", 1, 1, "Drama")

        assertEquals(listOf("Searchable movie"), catalogStore.searchMovies("searchable").map { it.title })
        assertEquals(listOf("Searchable series"), catalogStore.searchSeries("searchable").map { it.title })
        assertNull(catalogStore.getMovieById(Long.MIN_VALUE))
    }
}
