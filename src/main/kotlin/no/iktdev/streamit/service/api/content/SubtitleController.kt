package no.iktdev.streamit.service.api.content

import no.iktdev.streamit.library.db.tables.content.SubtitleTable
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.shared.Mode
import no.iktdev.streamit.shared.RequiresAuthentication
import no.iktdev.streamit.shared.classes.Subtitle
import no.iktdev.streamit.shared.database.queries.executeFindForVideo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ApiRestController
@RequestMapping("/subtitle")
class SubtitleController {

    @RequiresAuthentication(Mode.Soft)
    @GetMapping(
        value = [
            "/movie/{title}",
            "/movie/{title}/{format}"
        ]
    )
    fun movieSubtitle(@PathVariable title: String, @PathVariable format: String? = null): List<Subtitle> {
        return if (format.isNullOrEmpty()) SubtitleTable.executeFindForVideo(title)
        else SubtitleTable.executeFindForVideo(title, format)
    }

    @RequiresAuthentication(Mode.Soft)
    @GetMapping(
        value = [
            "/serie/{collection}",
            "/serie/{collection}/{format}"
        ]
    )
    fun serieSubtitle(@PathVariable collection: String, @PathVariable format: String? = null): List<Subtitle> {
        return if (format.isNullOrEmpty()) SubtitleTable.executeFindForVideo(collection)
        else SubtitleTable.executeFindForVideo(collection, format)
    }

    @RequiresAuthentication(Mode.Soft)
    @GetMapping("/{name}")
    fun anySubtitle(@PathVariable name: String): List<Subtitle> {
        return SubtitleTable.executeFindForVideo(name)
    }
    @RequiresAuthentication(Mode.Soft)
    @GetMapping("/{name}/{format}")
    fun anySubtitleOnFormat(@PathVariable name: String, @PathVariable format: String): List<Subtitle> {
        return SubtitleTable.executeFindForVideo(name, format)
    }

}