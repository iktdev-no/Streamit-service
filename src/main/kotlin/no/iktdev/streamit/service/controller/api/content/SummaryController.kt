package no.iktdev.streamit.service.controller.api.content

import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.model.shared.content.Summary
import no.iktdev.streamit.service.stores.summary.ISummaryStore
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@ApiRestController
@RequestMapping("/v1/summary")
open class SummaryController(
    private val summaryStore: ISummaryStore
) {

    @RequiresAuthentication(Scope.CatalogRead)
    @GetMapping("/catalog/{id}")
    open fun getSummaryById(@PathVariable id: Long): List<Summary> {
        return summaryStore.getAll(id)
    }
}