package no.iktdev.streamit.service.stores

import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.TestContentData
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.stores.subtitle.ISubtitleStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.jetbrains.exposed.sql.selectAll
import no.iktdev.streamit.service.db.tables.content.v2.SubtitleTableV2

class SubtitleStoreTest : TestBaseWithDatabase() {
    @Autowired
    lateinit var subtitleStore: ISubtitleStore

    private var videoId: Long = 0

    @BeforeEach
    fun setUpFixture() {
        withTransaction { clearTables() }
        videoId = requireNotNull(TestContentData.movie("Subtitle test movie").videoId)
    }

    @Test
    fun `insert returns subtitle data and update persists changes`() {
        val inserted = subtitleStore.insert(videoId, "eng", "srt", "movie.en.srt")
        assertEquals("movie.en.srt", inserted.file)

        val updated = subtitleStore.update(inserted.id.toLong(), "nob", "vtt", "movie.no.vtt")
        assertEquals("nob", updated?.language)
        assertEquals("vtt", updated?.format)
        assertEquals("movie.no.vtt", updated?.file)

        val row = withTransaction {
            SubtitleTableV2.selectAll().single()
        }.getOrThrow()
        assertEquals(videoId, row[SubtitleTableV2.videoId].value)
        assertEquals("movie.no.vtt", row[SubtitleTableV2.file])
    }

    @Test
    fun `delete removes subtitle and update of missing id returns null`() {
        val inserted = subtitleStore.insert(videoId, "eng", "srt", "movie.en.srt")
        subtitleStore.delete(inserted.id.toLong())

        assertNull(subtitleStore.update(inserted.id.toLong(), "eng", "srt", "missing.srt"))
        val rows = withTransaction { SubtitleTableV2.selectAll().count() }.getOrThrow()
        assertEquals(0L, rows)
    }
}
