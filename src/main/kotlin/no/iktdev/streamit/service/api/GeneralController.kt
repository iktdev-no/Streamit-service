package no.iktdev.streamit.service.api

import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.services.ConfigValueService
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.dto.CapabilitiesObject
import no.iktdev.streamit.service.dto.Heartbeat
import no.iktdev.streamit.service.dto.Server
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import jakarta.servlet.http.HttpServletRequest

@ApiRestController
@RequestMapping()
class GeneralController(
    @Autowired val config: ConfigValueService
) {

    @RequiresAuthentication(Scope.None)
    @GetMapping("/heartbeat")
    fun heartbeatPath(): Heartbeat {
        return Heartbeat(true, System.currentTimeMillis() / 1000L)
    }

    @RequiresAuthentication(Scope.AuthorizedRead)
    @GetMapping()
    fun main(request: HttpServletRequest? = null): ResponseEntity<String> {
        return ResponseEntity.ok().body(null)
    }

    @GetMapping("/capabilities")
    fun capabilities(): ResponseEntity<CapabilitiesObject> {
        return ResponseEntity.ok().body(CapabilitiesObject)
    }

    @GetMapping("/info/id")
    fun serverId(): ResponseEntity<String> {
        val serverId = config.serverId
        return if (serverId.isNullOrBlank()) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok(serverId)
        }
    }

    @GetMapping("/info/server")
    fun serverInfo(): ResponseEntity<Server?> {
        val server = config.server ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        return ResponseEntity.ok(server)
    }

    @GetMapping("/info/serverqr")
    fun serverQR(): ResponseEntity<ByteArray> {

        val image = config.generateQRCode()
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "PNG", outputStream)

        return ResponseEntity.ok()
            .header("Content-Type", "image/png")
            .body(outputStream.toByteArray())
    }

}