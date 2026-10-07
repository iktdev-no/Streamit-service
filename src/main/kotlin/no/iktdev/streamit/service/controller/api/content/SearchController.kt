package no.iktdev.streamit.service.controller.api.content

import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.model.shared.content.Catalog
import no.iktdev.streamit.service.stores.catalog.ICatalogStore
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@ApiRestController
@RequestMapping("/v1/search")
class SearchController(
    private val catalog: ICatalogStore
) {

    @RequiresAuthentication(Scope.CatalogRead)
    @GetMapping("/movie/{keyword}")
    fun movieSearch(@PathVariable keyword: String? = null): List<Catalog> {
        return if (!keyword.isNullOrEmpty())
            catalog.searchMovies(keyword) else emptyList()
    }

    @RequiresAuthentication(Scope.CatalogRead)
    @GetMapping("/serie/{keyword}")
    open fun serieSearch(@PathVariable keyword: String?): List<Catalog> {
        return if (!keyword.isNullOrEmpty())
            catalog.searchSeries(keyword) else emptyList()
    }

    @RequiresAuthentication(Scope.CatalogRead)
    @GetMapping("/{keyword}")
    open fun search(@PathVariable keyword: String?): List<Catalog> {
        return if (!keyword.isNullOrEmpty())
            catalog.search(keyword) else emptyList()
    }

}