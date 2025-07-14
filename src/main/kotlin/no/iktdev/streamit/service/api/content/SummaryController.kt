package no.iktdev.streamit.service.api.content

import no.iktdev.streamit.library.db.tables.content.SummaryTable
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.shared.Mode
import no.iktdev.streamit.shared.RequiresAuthentication
import no.iktdev.streamit.shared.classes.Summary
import no.iktdev.streamit.shared.database.queries.executeSelectOnId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ApiRestController
@RequestMapping("/summary")
open class SummaryController {

    @RequiresAuthentication(Mode.Soft)
    @GetMapping("/{id}")
    open fun getSummaryById(@PathVariable id: Int): List<Summary> {
        return if (id > -1) SummaryTable.executeSelectOnId(id) else emptyList()
    }
}