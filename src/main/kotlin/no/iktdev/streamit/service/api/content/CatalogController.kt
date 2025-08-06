package no.iktdev.streamit.service.api.content

import mu.KotlinLogging
import no.iktdev.streamit.library.db.tables.content.*
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.shared.RequiresAuthentication
import no.iktdev.streamit.shared.Scope
import no.iktdev.streamit.shared.classes.Catalog
import no.iktdev.streamit.shared.classes.GenreCatalog
import no.iktdev.streamit.shared.classes.Movie
import no.iktdev.streamit.shared.classes.Serie
import no.iktdev.streamit.shared.database.queries.*
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