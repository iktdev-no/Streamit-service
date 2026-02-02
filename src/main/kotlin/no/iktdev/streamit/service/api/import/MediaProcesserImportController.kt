package no.iktdev.streamit.service.api.import

import mu.KotlinLogging
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.db.tables.content.CatalogTable
import no.iktdev.streamit.service.db.tables.content.GenreTable
import no.iktdev.streamit.service.db.tables.content.MovieTable
import no.iktdev.streamit.service.db.tables.content.SerieTable
import no.iktdev.streamit.service.db.tables.content.SubtitleTable
import no.iktdev.streamit.service.db.tables.content.SummaryTable
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.dto.MediaProcesserImport
import no.iktdev.streamit.service.services.ImportContentService
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnoreAndGetId
import org.jetbrains.exposed.sql.update
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import java.time.LocalDateTime

@ApiRestController
@RequestMapping("/mediaprocesser")
class MediaProcesserImportController(
    private val importContentService: ImportContentService
) {

    val log = KotlinLogging.logger {}

    @RequiresAuthentication(Scope.MediaWrite)
    @PostMapping("/import")
    fun import(@RequestBody import: MediaProcesserImport): ResponseEntity<Void> {
        val success = importContentService.importContent(import)
        return if (success) {
            ResponseEntity(HttpStatus.OK)
        } else {
            log.warn("Import failed for collection=${import.collection}, title=${import.metadata.title}")
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

}