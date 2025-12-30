package no.iktdev.streamit.service.api.content

import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.db.tables.content.SubtitleTable
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.dto.Subtitle
import no.iktdev.streamit.service.db.queries.executeFindForVideo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@ApiRestController
@RequestMapping("/subtitle")
class SubtitleController {

    @RequiresAuthentication(Scope.CatalogRead)
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

    @RequiresAuthentication(Scope.CatalogRead)
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

    @RequiresAuthentication(Scope.CatalogRead)
    @GetMapping("/{name}")
    fun anySubtitle(@PathVariable name: String): List<Subtitle> {
        return SubtitleTable.executeFindForVideo(name)
    }
    @RequiresAuthentication(Scope.CatalogRead)
    @GetMapping("/{name}/{format}")
    fun anySubtitleOnFormat(@PathVariable name: String, @PathVariable format: String): List<Subtitle> {
        return SubtitleTable.executeFindForVideo(name, format)
    }

}