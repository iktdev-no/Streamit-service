package no.iktdev.streamit.service.api.content

import no.iktdev.streamit.library.db.tables.content.CatalogTable
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.shared.Mode
import no.iktdev.streamit.shared.RequiresAuthentication
import no.iktdev.streamit.shared.classes.Catalog
import no.iktdev.streamit.shared.database.queries.executeSearchForMovie
import no.iktdev.streamit.shared.database.queries.executeSearchForSerie
import no.iktdev.streamit.shared.database.queries.executeSearchWith
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ApiRestController
@RequestMapping("/search")
class SearchController {

    @RequiresAuthentication(Mode.Soft)
    @GetMapping("/movie/{keyword}")
    open fun movieSearch(@PathVariable keyword: String?): List<Catalog> {
        return if (!keyword.isNullOrEmpty())
            CatalogTable.executeSearchForMovie(keyword) else emptyList()
    }

    @RequiresAuthentication(Mode.Soft)
    @GetMapping("/serie/{keyword}")
    open fun serieSearch(@PathVariable keyword: String?): List<Catalog> {
        return if (!keyword.isNullOrEmpty())
            CatalogTable.executeSearchForSerie(keyword) else emptyList()
    }

    @RequiresAuthentication(Mode.Soft)
    @GetMapping("/{keyword}")
    open fun search(@PathVariable keyword: String?): List<Catalog> {
        return if (!keyword.isNullOrEmpty())
            CatalogTable.executeSearchWith(keyword) else emptyList()
    }

}