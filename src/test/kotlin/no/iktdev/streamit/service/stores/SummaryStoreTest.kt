package no.iktdev.streamit.service.stores

import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.TestContentData
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.model.shared.content.Summary
import no.iktdev.streamit.service.stores.summary.ISummaryStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SummaryStoreTest : TestBaseWithDatabase() {
    @Autowired
    lateinit var summaryStore: ISummaryStore

    private var catalogId: Long = 0

    @BeforeEach
    fun setUpFixture() {
        withTransaction { clearTables() }
        catalogId = TestContentData.movie("Summary test movie").catalogId
    }

    @Test
    fun `insert and getAll return summaries for one catalog ordered by language`() {
        val english = summaryStore.insert(catalogId, "English synopsis", "eng")
        val norwegian = summaryStore.insert(catalogId, "Norsk sammendrag", "nob")

        assertEquals(listOf(english, norwegian), summaryStore.getAll(catalogId))
        assertEquals(english, summaryStore.get(english.id.toLong()))
    }

    @Test
    fun `update changes summary and delete removes it`() {
        val summary = summaryStore.insert(catalogId, "Old synopsis", "eng")

        val updated = summaryStore.update(summary.id.toLong(), "Updated synopsis", "eng")
        assertEquals("Updated synopsis", updated?.description)
        assertEquals("eng", updated?.language)

        summaryStore.delete(summary.id.toLong())
        assertNull(summaryStore.get(summary.id.toLong()))
        assertEquals(emptyList<Summary>(), summaryStore.getAll(catalogId))
    }
}
