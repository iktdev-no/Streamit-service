package no.iktdev.streamit.service.controller.api.import

import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.model.shared.contentImport.MediaImportV2
import no.iktdev.streamit.service.services.ImportContentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@ApiRestController
@Tag(name = "Media import", description = "Import and upsert media catalog information")
@RequestMapping("/media/import")
class MediaImportController(
    private val importContentService: ImportContentService
) {

    val log = KotlinLogging.logger {}

    @RequiresAuthentication(Scope.MediaWrite)
    @PostMapping("/import")
    fun import(@RequestBody import: MediaImportV2): ResponseEntity<Void> {
        val success = importContentService.import(import)
        return if (success) {
            ResponseEntity(HttpStatus.OK)
        } else {
            log.warn("Import failed for collection=${import.reference?.store}, title=${import.metadata?.title}")
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

}
