package no.iktdev.streamit.service.assets

import mu.KotlinLogging
import no.iktdev.streamit.service.AssetRestController
import no.iktdev.streamit.service.ContentRestController
import no.iktdev.streamit.shared.Env
import no.iktdev.streamit.shared.Mode
import no.iktdev.streamit.shared.RequiresAuthentication
import no.iktdev.streamit.shared.with
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.FileInputStream
import java.io.OutputStream
import java.nio.file.Files

@AssetRestController
@RequestMapping()
open class AssetsController {
    val log = KotlinLogging.logger {}

    init {
        if (Env.getContentFolder() == null || Env.getContentFolder()?.exists() == false) {
            log.warn { "No content provided or exists.. No providing through controller will be available.." }
        }
    }

    @GetMapping("/profile-image/{image}")
    open fun getProfileImage(@PathVariable image: String): ResponseEntity<Resource> {
        val file = Env.getAssetsFolder().with(image)

        if (file.exists()) {
            val fileResource: Resource = FileSystemResource(file)
            val contentType = Files.probeContentType(file.toPath()) ?: MediaType.APPLICATION_OCTET_STREAM_VALUE
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"${fileResource.filename}\""
                )
                .body(fileResource)
        } else {
            return ResponseEntity.notFound().build()
        }
    }

}