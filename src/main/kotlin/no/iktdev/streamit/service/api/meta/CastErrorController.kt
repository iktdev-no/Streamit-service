package no.iktdev.streamit.service.api.meta

import com.google.gson.Gson
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.db.tables.info.CastErrorTable
import no.iktdev.streamit.service.dto.CastError
import no.iktdev.streamit.service.dto.Response
import no.iktdev.streamit.service.db.queries.executeInsert
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus

@ApiRestController
@RequestMapping("/cast")
class CastErrorController {

    @PostMapping("/error")
    @ResponseStatus(HttpStatus.OK)
    fun uploadedCastError(@RequestBody data: CastError) : ResponseEntity<String> {
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
        return ResponseEntity.ok(Gson().toJson(Response()))
    }


}