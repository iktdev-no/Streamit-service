package no.iktdev.streamit.service.controller.api.meta

import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.model.shared.CollectionReference
import no.iktdev.streamit.service.stores.title.ITitleStore
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@ApiRestController
@RequestMapping("/api/metadata")
class MetadataController(
    private val titleStore: ITitleStore,
) {

    @PostMapping("/catalog/search")
    fun searchForCollection(@RequestBody titles: List<String>): ResponseEntity<CollectionReference> {
        val titleMatch = titleStore.findCollectionByTitles(titles) ?: return ResponseEntity(HttpStatus.NO_CONTENT);
        return ResponseEntity.ok(titleMatch)
    }

}