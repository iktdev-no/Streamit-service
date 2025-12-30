package no.iktdev.streamit.service.api.content

import mu.KotlinLogging
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.db.queries.executeGetAll
import no.iktdev.streamit.service.db.queries.executeGetByIds
import no.iktdev.streamit.service.db.queries.executeGetCatalogGroupedByGenre
import no.iktdev.streamit.service.db.queries.executeGetMovies
import no.iktdev.streamit.service.db.queries.executeGetRecentlyAdded
import no.iktdev.streamit.service.db.queries.executeGetRecentlyUpdatedSeries
import no.iktdev.streamit.service.db.queries.executeGetSeries
import no.iktdev.streamit.service.db.queries.executeResumeOrNextEpisode
import no.iktdev.streamit.service.db.queries.executeSelectOn
import no.iktdev.streamit.service.db.queries.executeSelectOnId
import no.iktdev.streamit.service.db.tables.content.CatalogTable
import no.iktdev.streamit.service.db.tables.content.GenreTable
import no.iktdev.streamit.service.db.tables.content.ProgressTable
import no.iktdev.streamit.service.db.tables.content.SerieTable
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.db.tables.content.MovieTable
import no.iktdev.streamit.service.dto.Catalog
import no.iktdev.streamit.service.dto.GenreCatalog
import no.iktdev.streamit.service.dto.Movie
import no.iktdev.streamit.service.dto.Serie
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@ApiRestController
@RequestMapping("/catalog")
class CatalogController {
    val log = KotlinLogging.logger {}

    @GetMapping
    @RequiresAuthentication(Scope.CatalogRead)
    fun all(): List<Catalog> {
        log.info { "Processing '/catalog'" }
        return CatalogTable.executeGetAll()
    }

    @GetMapping("/get/{ids}")
    @RequiresAuthentication(Scope.CatalogRead)
    fun getCatalogById(@PathVariable ids: String): List<Catalog> {
        val intIds: List<Int> = ids.split(",").map { it.trim() }.mapNotNull { it.toIntOrNull() }
        return CatalogTable.executeGetByIds(intIds);
    }

    @GetMapping("/new")
    @RequiresAuthentication(Scope.CatalogRead)
    fun getNewContent(): List<Catalog> {
        log.info { "Processing '/catalog/new'" }
        return CatalogTable.executeGetRecentlyAdded()
    }

    @GetMapping("/movie")
    @RequiresAuthentication(Scope.CatalogRead)
    fun allMovies(): List<Catalog> {
        log.info { "Processing '/catalog/movie'" }
        return CatalogTable.executeGetMovies()
    }

    @GetMapping("/movie/{id}")
    @RequiresAuthentication(Scope.CatalogRead)
    fun movies(@PathVariable id: Int? = -1): Movie? {
        log.info { "Processing '/catalog/movie/id' where id is $id" }
        return if (id != null && id > -1) MovieTable.executeSelectOnId(id) else null
    }


    @GetMapping("/serie")
    @RequiresAuthentication(Scope.CatalogRead)
    fun allSeries(): List<Catalog> {
        log.info { "Processing '/catalog/serie" }
        return CatalogTable.executeGetSeries()
    }

    @GetMapping("/serie/{param}")
    @RequiresAuthentication(Scope.CatalogRead)
    fun getSerie(@PathVariable param: String): Serie? {
        return if (param.toIntOrNull() != null) {  // Hvis param er et tall, bruk det som id
            val id = param.toInt()
            log.info { "Processing '/catalog/serie/id' where id is $id" }
            if (id > -1) SerieTable.executeSelectOnId(id) else null
        } else {  // Ellers behandle det som en collection
            log.info { "Processing '/serie/collection' where collection is $param" }
            SerieTable.executeSelectOn(param)
        }
    }

    @GetMapping("/updated")
    @RequiresAuthentication(Scope.CatalogRead)
    fun getUpdatedSeries(): List<Catalog> {
        log.info { "Processing '/catalog/updated'" }
        return CatalogTable.executeGetRecentlyUpdatedSeries()
    }

    @GetMapping("/genre")
    @RequiresAuthentication(Scope.CatalogRead)
    fun getGenredCatalogs(): List<GenreCatalog> {
        log.info { "Processing '/catalog/genred'" }
        return GenreTable.executeGetCatalogGroupedByGenre()
    }

    @GetMapping("/{userId}/continue/serie")
    @RequiresAuthentication(Scope.CatalogRead)
    fun getContinueOrResumeSerie(@PathVariable userId: String): List<Serie> {
        log.info { "Processing '/catalog/userId/continue/serie' where userId is $userId" }
        return ProgressTable.executeResumeOrNextEpisode(userId)
    }
}