package no.iktdev.streamit.service.api.content

import mu.KotlinLogging
import no.iktdev.streamit.library.db.tables.content.CatalogTable
import no.iktdev.streamit.library.db.tables.content.FavoriteTable
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.shared.RequiresAuthentication
import no.iktdev.streamit.shared.Scope
import no.iktdev.streamit.shared.classes.Catalog
import no.iktdev.streamit.shared.database.queries.executeGetByIds
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@ApiRestController
@RequestMapping("/favorites")
class FavoritesApiController {
    val log = KotlinLogging.logger {}

    @GetMapping("/{userId}/ids")
    @RequiresAuthentication(Scope.UserRead)
    fun getFavorites(@PathVariable userId: String): List<Int> {
        return FavoriteTable.getFavorites(userId)
    }

    @GetMapping("/{userId}")
    @RequiresAuthentication(Scope.UserRead)
    fun getFavoriteCatalog(@PathVariable userId: String): List<Catalog> {
        val ids = FavoriteTable.getFavorites(userId)
        return CatalogTable.executeGetByIds(ids)
    }

    @PutMapping("/{userId}")
    @RequiresAuthentication(Scope.UserWrite)
    fun setFavoriteCatalogId(@PathVariable userId: String, @RequestBody catalogId: Int) {
        FavoriteTable.addFavorite(userId, catalogId)
    }

    @DeleteMapping("/{userId}")
    @RequiresAuthentication(Scope.UserWrite)
    fun removeFavoriteCatalogId(@PathVariable userId: String, @RequestBody catalogId: Int) {
        FavoriteTable.removeFavorite(userId, catalogId)
    }

}