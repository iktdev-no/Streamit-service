package no.iktdev.streamit.service.stream

import mu.KotlinLogging
import no.iktdev.streamit.service.ContentRestController
import no.iktdev.streamit.service.supporting.WebUtil
import no.iktdev.streamit.shared.Env
import no.iktdev.streamit.shared.Mode
import no.iktdev.streamit.shared.RequiresAuthentication
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
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import org.springframework.web.util.UrlPathHelper
import java.io.FileInputStream
import java.io.OutputStream
import java.nio.file.Files
import javax.servlet.http.HttpServletRequest

@ContentRestController
@RequestMapping(path = ["/media/"])
open class ContentController {
    val log = KotlinLogging.logger {}

    init {
        if (Env.getContentFolder() == null || Env.getContentFolder()?.exists() == false) {
            if (Env.getContentFolder() != null) {
                log.warn { "No content folder provided!" }
            }
            if (Env.getContentFolder()?.exists() == false) {
                log.error { "Failed to find folder ${Env.getContentFolder()?.absolutePath}" }
            }
        }
    }


    @RequiresAuthentication(Mode.Strict)
    @GetMapping("video/{collection}/{video}")
    open fun provideVideoFile(@PathVariable collection: String, @PathVariable video: String): ResponseEntity<Resource> {
        val file = Env.getContentFolder()?.with(collection, video)

        if (file?.exists() == true) {
            val fileResource: Resource = FileSystemResource(file)
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    ("attachment; filename=\"" + fileResource.filename) + "\""
                )
                .body(fileResource)
        } else {
            return ResponseEntity.notFound().build()
        }
    }

    @RequiresAuthentication(Mode.Strict)
    @GetMapping("image/{collection}/{image}")
    open fun provideImageFile(@PathVariable collection: String, @PathVariable image: String): ResponseEntity<ByteArray> {
        val file = Env.getContentFolder()?.with(collection, image)

        if (file?.exists() == true) {
            val contentType = when (file.extension.lowercase()) {
                "jpg", "jpeg" -> MediaType.IMAGE_JPEG
                "png" -> MediaType.IMAGE_PNG
                "bmp" -> MediaType.parseMediaType("image/bmp")
                "webp" -> MediaType.parseMediaType("image/webp")
                else -> MediaType.APPLICATION_OCTET_STREAM
            }
            return ResponseEntity.ok()
                .contentType(contentType)
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    ("attachment; filename=\"" + file.name) + "\""
                )
                .body(Files.readAllBytes(file.toPath()))
        }
        return ResponseEntity.notFound().build()

    }

    @RequiresAuthentication(Mode.Strict)
    @GetMapping("subtitle/{collection}/{language}/{subtitle}")
    open fun provideSubtitle(@PathVariable collection: String, @PathVariable language: String, @PathVariable subtitle: String): ResponseEntity<ByteArray> {
        val file = Env.getContentFolder()?.with(collection, "sub", language, subtitle)
        if (file?.exists() == true) {
            val contentType = when (file.extension.lowercase()) {
                "srt" -> MediaType.parseMediaType("application/x-subrip")
                "vtt" -> MediaType.parseMediaType("text/vtt")
                "ass", "ssa" -> MediaType.parseMediaType("text/x-ssa")
                "sub" -> MediaType.parseMediaType("text/plain") // Noen SUB-filer kan være binære, så sjekk formatet
                else -> MediaType.APPLICATION_OCTET_STREAM
            }

            return ResponseEntity.ok()
                .contentType(contentType)
                .body(Files.readAllBytes(file.toPath()))
        }
        return ResponseEntity.notFound().build()
    }

    @GetMapping("/browse/**", produces = [MediaType.TEXT_HTML_VALUE])
    fun browse(request: HttpServletRequest): ResponseEntity<String> {
        if (!isDebug()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("<h2 style='color:tomato;'>This endpoint is only available in debug mode</h2>")
        }

        val contentFileFolder = Env.getContentFolder()
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("<h2 style='color:tomato;'>Content folder is not configured</h2>")

        val basePath = contentFileFolder.toPath().normalize()
        val fullPath = UrlPathHelper().getPathWithinApplication(request)
        val mappingPrefix = "/stream/media/browse" // Update if reused in other controllers
        val subPath = fullPath.removePrefix(mappingPrefix).removePrefix("/").ifBlank { "" }
        val current = basePath.resolve(subPath).normalize()

        if (!Files.exists(current)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("<h2 style='color:tomato;'>Path not found: ${current}</h2>")
        }

        if (Files.isRegularFile(current)) {
            val route = routeToFile(subPath)
            return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, route)
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

    fun routeToFile(subPath: String): String {
        val segments = subPath.removePrefix("/").split("/")
        val ext = segments.last().substringAfterLast('.', missingDelimiterValue = "").lowercase()

        return when {
            segments.size == 2 && ext in listOf("jpg", "jpeg", "png", "webp") ->
                "/stream/media/image/${segments[0]}/${segments[1]}"
            segments.size == 2 && ext in listOf("mp4", "mkv", "avi") ->
                "/stream/media/video/${segments[0]}/${segments[1]}"
            segments.size == 4 && segments[1] == "subs" ->
                "/stream/media/subtitle/${segments[0]}/${segments[2]}/${segments[3]}"
            else -> "#"
        }
    }




}