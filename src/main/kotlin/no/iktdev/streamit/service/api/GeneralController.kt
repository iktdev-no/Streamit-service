package no.iktdev.streamit.service.api

import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.shared.Mode
import no.iktdev.streamit.shared.RequiresAuthentication
import no.iktdev.streamit.shared.classes.Heartbeat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletRequest

@ApiRestController
@RequestMapping("/api")
class GeneralController {

    @GetMapping("/heartbeat")
    fun heartbeatPath(): Heartbeat {
        return Heartbeat(true, System.currentTimeMillis() / 1000L)
    }

    @RequiresAuthentication(Mode.Strict)
    @GetMapping()
    fun main(request: HttpServletRequest? = null): ResponseEntity<String> {
        return ResponseEntity.ok().body(null)
    }
}