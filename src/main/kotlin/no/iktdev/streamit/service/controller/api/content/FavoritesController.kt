package no.iktdev.streamit.service.controller.api.content

import mu.KotlinLogging
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.model.shared.content.Catalog
import no.iktdev.streamit.service.stores.catalog.ICatalogStore
import no.iktdev.streamit.service.stores.favorite.IFavoriteStore
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@ApiRestController
@RequestMapping("/v1/favorites")
class FavoritesController(
    private val favoriteStore: IFavoriteStore,
    private val catalogStore: ICatalogStore,
) {
    val log = KotlinLogging.logger {}

    @GetMapping("/{userId}/ids")
    @RequiresAuthentication(Scope.UserRead)
    fun getFavorites(@PathVariable userId: String): List<Long> {
        return favoriteStore.getAll(userId)
    }

    @GetMapping("/{userId}")
    @RequiresAuthentication(Scope.UserRead)
    fun getFavoriteCatalog(@PathVariable userId: String): List<Catalog> {
        val ids = favoriteStore.getAll(userId)
        return catalogStore.getByIds(ids)
    }

    @PutMapping("/{userId}")
    @RequiresAuthentication(Scope.UserWrite)
    fun setFavoriteCatalogId(@PathVariable userId: String, @RequestBody catalogId: Long) {
        favoriteStore.add(userId, catalogId)
    }

    @DeleteMapping("/{userId}")
    @RequiresAuthentication(Scope.UserWrite)
    fun removeFavoriteCatalogId(@PathVariable userId: String, @RequestBody catalogId: Long) {
        favoriteStore.remove(userId, catalogId)
    }

}