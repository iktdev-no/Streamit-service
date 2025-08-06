package no.iktdev.streamit.service.api.content

import no.iktdev.streamit.library.db.tables.content.GenreTable
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.shared.RequiresAuthentication
import no.iktdev.streamit.shared.Scope
import no.iktdev.streamit.shared.classes.Genre
import no.iktdev.streamit.shared.database.queries.executeGetAll
import no.iktdev.streamit.shared.database.queries.executeSelectById
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@ApiRestController
@RequestMapping("/genre")
class GenreController {

    @RequiresAuthentication(Scope.CatalogRead)
    @GetMapping("")
    fun genres(): List<Genre> {
        return GenreTable.executeGetAll()
    }

    @RequiresAuthentication(Scope.CatalogRead)
    @GetMapping("/{id}")
    fun genre(@PathVariable id: Int = 0): Genre? {
        return GenreTable.executeSelectById(id)
    }
}