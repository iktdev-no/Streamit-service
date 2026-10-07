package no.iktdev.streamit.service.controller.api.content

import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.model.shared.content.Genre
import no.iktdev.streamit.service.stores.genre.IGenreStore
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@ApiRestController
@RequestMapping("/v1/genre")
class GenreController(
    private val genres: IGenreStore
) {

    @RequiresAuthentication(Scope.CatalogRead)
    @GetMapping("")
    fun genres(): List<Genre> {
        return genres.getAll()
    }

    @RequiresAuthentication(Scope.CatalogRead)
    @GetMapping("/{id}")
    fun genre(@PathVariable id: Long): Genre? {
        return genres.getById(id)
    }
}