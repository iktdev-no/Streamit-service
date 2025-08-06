package no.iktdev.streamit.service.assets

import mu.KotlinLogging
import no.iktdev.streamit.service.AssetRestController
import no.iktdev.streamit.service.supporting.WebUtil
import no.iktdev.streamit.shared.Env
import no.iktdev.streamit.shared.isDebug
import no.iktdev.streamit.shared.with
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.util.UrlPathHelper
import java.nio.file.Files
import javax.servlet.http.HttpServletRequest

@AssetRestController
@RequestMapping()
open class AssetsController {
    val log = KotlinLogging.logger {}

    init {
        if (!Env.getAssetsFolder().exists()) {
            if (Env.getAssetsFolder().mkdirs()) {
                log.info { "Assets folder created at ${Env.getAssetsFolder()}" }
            } else {
                log.error { "Failed to create assets folder at ${Env.getAssetsFolder()}" }
            }
            if (!Env.getAssetsFolder().exists()) {
                log.warn { "No content provided or exists.. No providing through controller will be available.." }
            }
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

    private val urlPathHelper = UrlPathHelper()
    @GetMapping("/browse/**", produces = [MediaType.TEXT_HTML_VALUE])
    fun browse(request: HttpServletRequest): ResponseEntity<String> {
        if (!isDebug()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("<h2 style='color:tomato;'>This endpoint is only available in debug mode</h2>")
        }

        val basePath = Env.getAssetsFolder().toPath().normalize()
        val fullPath = UrlPathHelper().getPathWithinApplication(request)
        val mappingPrefix = "/assets/browse"
        val subPath = fullPath.removePrefix(mappingPrefix).removePrefix("/").ifBlank { "" }
        val current = basePath.resolve(subPath).normalize()

        if (!Files.exists(current) || !current.startsWith(basePath)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("<h2 style='color:tomato;'>Path not found: $current</h2>")
        }

        if (Files.isRegularFile(current)) {
            val filename = current.fileName.toString()
            return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/assets/profile-image/$filename")
                .build()
        }


        val entries = Files.list(current)
            .map { it.fileName.toString() to Files.isDirectory(it) }
            .toList()
            .sortedBy { it.first.lowercase() }

        val html = WebUtil().renderDirectoryListing(
            baseUrl = "$mappingPrefix/",
            pathPrefix = basePath.toString(),
            currentPath = current,
            entries = entries
        )

        return ResponseEntity.ok(html)
    }


}