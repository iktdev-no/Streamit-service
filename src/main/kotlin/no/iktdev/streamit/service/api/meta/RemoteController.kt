package no.iktdev.streamit.service.api.meta

import mu.KotlinLogging
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.services.ConfigValueService
import no.iktdev.streamit.service.services.PfnsClientService
import no.iktdev.streamit.shared.classes.pfns.ServerConfigPushObject
import no.iktdev.streamit.shared.classes.pfns.PfnsRemoteServerObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@ApiRestController
@RequestMapping("/api/remote")
class RemoteController(
    @Autowired var service: PfnsClientService,
    @Autowired var config: ConfigValueService
    ) {
    val log = KotlinLogging.logger {}


    @PostMapping("/push/configure/server")
    open fun sendConfigurationForServer(@RequestBody data: ServerConfigPushObject) {
        val serverId = config.serverId
        if (serverId.isNullOrBlank()) {
            log.error { "Server ID is not set. Cannot send configuration." }
            return
        }
        val pfnsServer = PfnsRemoteServerObject(
            serverId = serverId,
            pfnsReceiverId = data.receiverId,
            server = data.server
        )

        service.sendServerConfiguration(pfnsServer)
    }

}