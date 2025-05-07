package no.iktdev.streamit.service.api.meta

import com.google.gson.Gson
import no.iktdev.streamit.library.db.tables.other.CastErrorTable
import no.iktdev.streamit.shared.classes.CastError
import no.iktdev.streamit.shared.classes.Response
import no.iktdev.streamit.shared.database.queries.executeInsert
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/cast")
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