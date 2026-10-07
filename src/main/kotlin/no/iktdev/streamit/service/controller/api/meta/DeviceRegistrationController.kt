package no.iktdev.streamit.service.controller.api.meta

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.services.ConfigValueService
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.dto.CapabilitiesObject
import no.iktdev.streamit.service.dto.RegisterDeviceData
import no.iktdev.streamit.service.stores.device.IDeviceRegistrationStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@ApiRestController
@Tag(name = "Device registration", description = "Register and manage trusted client devices")
@RequestMapping("/device/registration")
class DeviceRegistrationController(
    private val config: ConfigValueService,
    private val deviceRegistrationStore: IDeviceRegistrationStore
) {

    @PostMapping("/register")
    @RequiresAuthentication(Scope.None)
    @Operation(
        summary = "Register a device",
        description = "Unauthenticated bootstrap registration. Available only when remote configuration is enabled."
    )
    open fun register(@RequestBody device: RegisterDeviceData): ResponseEntity<String> {
        if (!CapabilitiesObject.remoteConfigurationAvailable) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Remote configuration is not available on this server.")
        }

        val status = deviceRegistrationStore.register(device)
        if (status) {
            return ResponseEntity.ok().build()
        }

        return ResponseEntity.unprocessableEntity().build()
    }

    @GetMapping("/list")
    @RequiresAuthentication(Scope.DeviceRegistryRead)
    fun getRegisteredDevices(): ResponseEntity<String> {
        return ResponseEntity.noContent().build()
    }

}
