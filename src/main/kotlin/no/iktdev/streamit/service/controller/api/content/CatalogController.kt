package no.iktdev.streamit.service.controller.api.content

import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.Env
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.model.shared.content.Catalog
import no.iktdev.streamit.service.model.shared.content.GenreCatalog
import no.iktdev.streamit.service.model.shared.content.Movie
import no.iktdev.streamit.service.model.shared.content.Serie
import no.iktdev.streamit.service.stores.catalog.ICatalogStore
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@ApiRestController
@Tag(name = "Catalog", description = "Browse movies, series, episodes, and catalog changes")
@RequestMapping("/v1/catalog")
class CatalogController(
    private val catalog: ICatalogStore
) {
    val log = KotlinLogging.logger {}

    @GetMapping
    @RequiresAuthentication(Scope.CatalogRead)
    fun all(): List<Catalog> {
        log.info { "Processing '/catalog'" }
        return catalog.getAll(0, null)
    }

    @GetMapping("/get/{ids}")
    @RequiresAuthentication(Scope.CatalogRead)
    fun getCatalogById(@PathVariable ids: String): List<Catalog> {
        val intIds: List<Long> = ids.split(",").map { it.trim() }.mapNotNull { it.toLongOrNull() }
        return catalog.getByIds(intIds)
    }

    @GetMapping("/new")
    @RequiresAuthentication(Scope.CatalogRead)
    fun getNewContent(): List<Catalog> {
        val freshness = Env.frshness
        return catalog.getRecentlyAdded(freshness)
    }

    @GetMapping("/movie")
    @RequiresAuthentication(Scope.CatalogRead)
    fun allMovies(): List<Catalog> {
        return catalog.getMovies()
    }

    @GetMapping("/movie/{id}")
    @RequiresAuthentication(Scope.CatalogRead)
    fun movies(@PathVariable id: Long): Movie? {
        return catalog.getMovieById(id)
    }

    @GetMapping("/movie/{id}/for/{userId}")
    @RequiresAuthentication(Scope.CatalogRead)
    fun movieWithProgress(
        @PathVariable id: Long,
        @PathVariable userId: UUID
    ): Movie? {
        return catalog.getMovieByIdWithProgress(id, userId)
    }

    @GetMapping("/serie")
    @RequiresAuthentication(Scope.CatalogRead)
    fun allSeries(): List<Catalog> {
        return catalog.getSeries()
    }

    @GetMapping("/serie/{id}")
    @RequiresAuthentication(Scope.CatalogRead)
    fun series(@PathVariable id: Long): Serie? {
        return catalog.getSerieById(id)
    }

    @GetMapping("/serie/{id}/for/{userId}")
    @RequiresAuthentication(Scope.CatalogRead)
    fun seriesWithProgress(
        @PathVariable id: Long,
        @PathVariable userId: UUID
    ): Serie? {
        return catalog.getSerieByIdWithProgress(id, userId)
    }

    @GetMapping("/serie/{store}")
    @RequiresAuthentication(Scope.CatalogRead)
    fun serieByStore(@PathVariable store: String): Serie? {
        return catalog.getSerieByStore(store)
    }

    @GetMapping("/updated")
    @RequiresAuthentication(Scope.CatalogRead)
    fun getUpdatedSeries(): List<Catalog> {
        return catalog.getRecentlyUpdatedSeries()
    }

    @GetMapping("/genre")
    @RequiresAuthentication(Scope.CatalogRead)
    fun getGenredCatalogs(
        @RequestParam genreId: Long,
        @RequestParam(defaultValue = "0") offset: Long,
        @RequestParam(defaultValue = "100") limit: Int
    ): List<Catalog> {
        log.info { "Processing '/catalog/genre' genreId=$genreId offset=$offset limit=$limit" }
        return catalog.getByGenre(
            genreId = genreId,
            offset = offset,
            limit = limit
        )
    }

    @GetMapping("/{userId}/continue/serie")
    @RequiresAuthentication(Scope.CatalogRead)
    fun getContinueOrResumeSerie(@PathVariable userId: UUID): List<Serie> {
        return catalog.getContinueOrResumeSerie(userId)
    }
}
