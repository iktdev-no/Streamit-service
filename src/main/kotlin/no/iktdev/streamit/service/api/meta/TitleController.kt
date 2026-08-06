package no.iktdev.streamit.service.api.meta

import mu.KotlinLogging
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.db.tables.content.CatalogTable
import no.iktdev.streamit.service.services.TitleSearchService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@ApiRestController
@RequestMapping("/meta/title")
class TitleController(
    private var titleSearchService: TitleSearchService
) {
    val log = KotlinLogging.logger {}


    @RequiresAuthentication(Scope.CatalogRead)
    @PostMapping("/search")
    fun searchForCollection(@RequestBody name: String): ResponseEntity<String?> {
        val masterTitle = titleSearchService.findMasterBySanitized(name, null, null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Title not found")
        val collection = CatalogTable.findCollectionFrom(masterTitle)
        return if (collection.isNullOrEmpty()) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Title not found")
        } else ResponseEntity.status(HttpStatus.OK).body(collection)
    }

    @RequiresAuthentication(Scope.CatalogRead)
    @PostMapping("/search/batch")
    fun searchForCollections(@RequestBody names: List<String>): ResponseEntity<String> {
        val title = titleSearchService.batchSearch(names) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Title not found")
        val collection = CatalogTable.findCollectionFrom(title)

        return if (collection.isNullOrEmpty()) {
            log.error { "Collection not found i catalog using $title" }
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Title not found")
        } else {
            log.info("Collection found $collection in catalog using $title")
            ResponseEntity.status(HttpStatus.OK).body(collection)
        }
    }

}