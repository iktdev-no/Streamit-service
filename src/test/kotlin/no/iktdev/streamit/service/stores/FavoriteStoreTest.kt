package no.iktdev.streamit.service.stores

import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.TestContentData
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.model.internal.user.UserData
import no.iktdev.streamit.service.stores.favorite.IFavoriteStore
import no.iktdev.streamit.service.stores.user.IUserStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class FavoriteStoreTest : TestBaseWithDatabase() {
    @Autowired
    lateinit var favoriteStore: IFavoriteStore

    @Autowired
    lateinit var userStore: IUserStore

    private lateinit var userId: String
    private var catalogId: Long = 0

    @BeforeEach
    fun setUpFixture() {
        withTransaction { clearTables() }
        userId = userStore.insert(UserData("Favorite-${UUID.randomUUID()}", "Default-0.png")).uid
        catalogId = TestContentData.movie("Favorite test movie").catalogId
    }

    @Test
    fun `add is idempotent and getAll returns catalog ids`() {
        favoriteStore.add(userId, catalogId)
        favoriteStore.add(userId, catalogId)

        assertEquals(listOf(catalogId), favoriteStore.getAll(userId))
    }

    @Test
    fun `remove deletes only the selected favorite`() {
        val secondCatalogId = TestContentData.movie("Another favorite").catalogId
        favoriteStore.add(userId, catalogId)
        favoriteStore.add(userId, secondCatalogId)

        favoriteStore.remove(userId, catalogId)

        assertEquals(listOf(secondCatalogId), favoriteStore.getAll(userId))
    }

    @Test
    fun `unknown users have no favorites and cannot add one`() {
        val unknownUserId = UUID.randomUUID().toString()
        favoriteStore.add(unknownUserId, catalogId)

        assertEquals(emptyList<Long>(), favoriteStore.getAll(unknownUserId))
        assertEquals(emptyList<Long>(), favoriteStore.getAll(userId))
    }
}
