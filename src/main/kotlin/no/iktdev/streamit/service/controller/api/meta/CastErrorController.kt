package no.iktdev.streamit.service.controller.api.meta

import io.swagger.v3.oas.annotations.tags.Tag
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.db.tables.info.CastErrorTable
import no.iktdev.streamit.service.model.shared.CastError
import no.iktdev.streamit.service.db.queries.executeInsert
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus

@ApiRestController
@Tag(name = "Cast diagnostics", description = "Submit casting diagnostic reports")
@RequestMapping("/cast")
class CastErrorController {

    @PostMapping("/error")
    @RequiresAuthentication(Scope.None)
    @ResponseStatus(HttpStatus.OK)
    fun uploadedCastError(@RequestBody data: CastError): ResponseEntity<Nothing> {
        CastErrorTable.executeInsert(
            deviceOsVersion = data.deviceAndroidVersion,
            castDeviceName = data.castDeviceName,
            appVersion = data.appVersion,
            file = data.file,
            deviceBrand = data.deviceBrand,
            deviceModel = data.deviceModel,
            deviceManufacturer = data.deviceManufacturer,
            error = data.error
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(null)
    }


}
