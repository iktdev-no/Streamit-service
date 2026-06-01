package no.iktdev.streamit.service.api.meta

import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.db.tables.content.CatalogTable
import no.iktdev.streamit.service.services.TitleSearchService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@ApiRestController
@RequestMapping("/meta/title")
class TitleController(
    private var titleSearchService: TitleSearchService
) {

    @RequiresAuthentication(Scope.CatalogRead)
    @GetMapping("/search/{name}")
    fun searchForCollection(@PathVariable name: String): ResponseEntity<String?> {
        val masterTitle = titleSearchService.findMasterBySanitized(name, null, null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Title not found")
        val collection = CatalogTable.findCollectionFrom(masterTitle)
        return if (collection.isNullOrEmpty()) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Title not found")
        } else ResponseEntity.status(HttpStatus.OK).body(collection)
    }

}