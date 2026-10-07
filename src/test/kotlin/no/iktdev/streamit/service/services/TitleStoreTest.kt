package no.iktdev.streamit.service.services

import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.TestContentData
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.stores.title.ITitleStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TitleStoreTest : TestBaseWithDatabase() {
    @Autowired
    lateinit var titleStore: ITitleStore

    private var catalogId: Long = 0

    @BeforeEach
    fun seedCatalogTitles() {
        withTransaction { clearTables() }
        catalogId = TestContentData.movie("Amélie", store = "movies").catalogId
        titleStore.insert(catalogId, "The Fabulous Destiny of Amélie Poulain", "eng", preferred = false)
    }

    @Test
    fun `findCollectionByTitles matches normalized alternative titles`() {
        val match = titleStore.findCollectionByTitles(listOf("the fabulous destiny of amelie poulain"))

        assertEquals(catalogId, match?.catalogId)
        assertEquals("movies", match?.store)
    }

    @Test
    fun `findCollectionByTitles ignores blank candidates`() {
        assertNull(titleStore.findCollectionByTitles(listOf(" ", "")))
    }

    @Test
    fun `findCollectionByTitles returns no match when titles identify multiple catalogs`() {
        val secondCatalogId = TestContentData.movie("The Other Amélie", store = "other").catalogId
        titleStore.insert(secondCatalogId, "Amélie", "eng", preferred = false)

        assertNull(titleStore.findCollectionByTitles(listOf("Amelie")))
    }
}
