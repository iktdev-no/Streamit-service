package no.iktdev.streamit.service.api.meta

import no.iktdev.streamit.library.db.executeWithStatus
import no.iktdev.streamit.library.db.tables.authentication.RegisteredDevicesTable
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.services.ConfigValueService
import no.iktdev.streamit.shared.Mode
import no.iktdev.streamit.shared.RequiresAuthentication
import no.iktdev.streamit.shared.classes.CapabilitiesObject
import no.iktdev.streamit.shared.classes.RegisterDeviceData
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@ApiRestController
@RequestMapping("/api/device/registration")
class DeviceRegistrationController(
    @Autowired val config: ConfigValueService
) {

    @PostMapping("/register")
    open fun register(@RequestBody device: RegisterDeviceData): ResponseEntity<String> {
        if (!CapabilitiesObject.remoteConfigurationAvailable) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Remote configuration is not available on this server.")
        }

        val status = executeWithStatus {
            RegisteredDevicesTable.insert {
                it[deviceId] = device.deviceId
                it[applicationPackageName] = device.applicationPackageName
                it[osVersion] = device.osVersion
                it[osPlatform] = device.osPlatform
            }
        }
        if (status) {
            return ResponseEntity.ok().build()
        }

        return ResponseEntity.unprocessableEntity().build()
    }

    @PostMapping("/replace/{oldToken}/set")
    open fun registerNewToken(@PathVariable oldToken: String, @RequestBody newToken: String): ResponseEntity<String> {
        if (!CapabilitiesObject.remoteConfigurationAvailable) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Remote configuration is not available on this server.")
        }

        val status = executeWithStatus {
            RegisteredDevicesTable.update({RegisteredDevicesTable.deviceId eq oldToken}) {
                it[deviceId] = newToken
            }
        }
        if (status) {
            return ResponseEntity.ok().build()
        }
        return ResponseEntity.badRequest().build()
    }

    @GetMapping("/list")
    @RequiresAuthentication(Mode.Strict)
    fun getRegisteredDevices(): ResponseEntity<String> {
        return ResponseEntity.noContent().build()
    }

}