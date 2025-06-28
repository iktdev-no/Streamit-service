package no.iktdev.streamit.service.api.content

import mu.KotlinLogging
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.shared.Env
import no.iktdev.streamit.shared.getOnlyFiles
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@ApiRestController
@RequestMapping("/api/assets")
class AssetsApiController {
    val log = KotlinLogging.logger {}

    @GetMapping("/profile-image")
    fun profileImage(): List<String> {
        log.info { "Processing '/assets/profile-image'" }
        return Env.getAssetsFolder().getOnlyFiles().filter { it.nameWithoutExtension.isNotBlank() }.map { it.name }
    }
}