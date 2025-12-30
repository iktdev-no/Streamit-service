package no.iktdev.streamit.service.api.content

import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.dto.Summary
import no.iktdev.streamit.service.db.queries.executeSelectOnId
import no.iktdev.streamit.service.db.tables.content.SummaryTable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@ApiRestController
@RequestMapping("/summary")
open class SummaryController {

    @RequiresAuthentication(Scope.CatalogRead)
    @GetMapping("/{id}")
    open fun getSummaryById(@PathVariable id: Int): List<Summary> {
        return if (id > -1) SummaryTable.executeSelectOnId(id) else emptyList()
    }
}