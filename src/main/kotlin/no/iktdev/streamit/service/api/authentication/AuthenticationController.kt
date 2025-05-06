package no.iktdev.streamit.service.api.authentication

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.streamit.library.db.executeWithStatus
import no.iktdev.streamit.library.db.tables.authentication.DelegatedAuthenticationTable
import no.iktdev.streamit.library.db.toEpochSeconds
import no.iktdev.streamit.library.db.withTransaction
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.shared.Authentication
import no.iktdev.streamit.shared.Mode
import no.iktdev.streamit.shared.RequiresAuthentication
import no.iktdev.streamit.shared.classes.Jwt
import no.iktdev.streamit.shared.classes.User
import no.iktdev.streamit.shared.classes.remote.AuthInitiateRequest
import no.iktdev.streamit.shared.classes.remote.DelegatedRequestData
import no.iktdev.streamit.shared.classes.remote.PermitRequestData
import no.iktdev.streamit.shared.classes.remote.RequestCreatedResponse
import no.iktdev.streamit.shared.classes.remote.RequestDeviceInfo
import no.iktdev.streamit.shared.database.queries.executeGetDelegatePendingRequestBy
import no.iktdev.streamit.shared.database.queries.executeInsertAndGetId
import no.iktdev.streamit.shared.getRequestersIp
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletRequest

@ApiRestController
@RequestMapping("/auth")
class AuthenticationController() {
    val auth = Authentication()
    val log = KotlinLogging.logger {}


    @RequiresAuthentication(Mode.Strict)
    @GetMapping(value = ["/validate"])
    fun validateToken(request: HttpServletRequest? = null): ResponseEntity<Boolean?> {
        val token = request?.getHeader("Authorization") ?: return ResponseEntity.internalServerError().body(null)
        val isValid = auth.isTokenValid(token)
        return if (isValid) {
            ResponseEntity.status(202).body(isValid)
        } else {
            ResponseEntity.status(405).body(isValid)
        }
    }

    @RequiresAuthentication(Mode.Strict)
    @GetMapping(value = ["/accessible"])
    fun validateAccessibilityToServer(request: HttpServletRequest? = null): ResponseEntity<String> {
        return ResponseEntity.ok().body(null)
    }

    @RequiresAuthentication(Mode.Strict)
    @PostMapping(value = ["/new"])
    fun createJWT(@RequestBody user: User): Jwt {
        return auth.createJwt(user)
    }

    @GetMapping(value = ["/delegate/required"])
    @RequiresAuthentication(Mode.None)
    fun doesRequireDelegate(): ResponseEntity<Boolean> {
        return ResponseEntity.ok(true)
    }

    /**
     * Creates a delegation request session for PIN-based authentication.
     *
     * This endpoint accepts an {@link AuthInitiateRequest} object in the request body
     * and initiates a delegation session specifically for "PIN" authentication.
     *
     * @param data The authentication initiation request containing necessary details.
     * @param request (Optional) The HTTP servlet request for context information.
     * @return A {@link ResponseEntity} containing a session identifier as a string.
     */
    @PostMapping(value = ["/delegate/request/pin"])
    @RequiresAuthentication(Mode.None)
    fun createPINDelegationRequestSession(@RequestBody data: AuthInitiateRequest, request: HttpServletRequest? = null): ResponseEntity<RequestCreatedResponse> {
        return createDelegationRequestSession(data, DelegatedAuthenticationTable.AuthMethod.PIN, request)
    }

    /**
     * Creates a delegation request session for QR code-based authentication.
     *
     * This endpoint accepts an {@link AuthInitiateRequest} object in the request body
     * and initiates a delegation session specifically for "QR" authentication.
     *
     * @param data The authentication initiation request containing necessary details.
     * @param request (Optional) The HTTP servlet request for context information.
     * @return A {@link ResponseEntity} containing a session identifier as a string.
     */
    @PostMapping(value = ["/delegate/request/qr"])
    @RequiresAuthentication(Mode.None)
    fun createQRDelegationRequestSession(@RequestBody data: AuthInitiateRequest, request: HttpServletRequest? = null): ResponseEntity<RequestCreatedResponse> {
        return createDelegationRequestSession(data, DelegatedAuthenticationTable.AuthMethod.QR, request)
    }


    fun createDelegationRequestSession(data: AuthInitiateRequest, pinOrQr: DelegatedAuthenticationTable.AuthMethod, request: HttpServletRequest?): ResponseEntity<RequestCreatedResponse> {
        val ip = request?.getRequestersIp()
        val reqId = data.toRequestId()
        var insertedId: Int? = null
        val success = executeWithStatus {
            insertedId = DelegatedAuthenticationTable.executeInsertAndGetId(
                pin = data.pin,
                requestId = data.toRequestId(),
                deviceInfo = Gson().toJson(data.deviceInfo),
                method = pinOrQr,
                ip = ip
            ).value
        }
        if (!success) {
            return ResponseEntity.unprocessableEntity().build()
        }
        val expires = withTransaction {
            DelegatedAuthenticationTable.select {
                DelegatedAuthenticationTable.id eq insertedId
            }.map { it[DelegatedAuthenticationTable.expires] }.firstOrNull()
        }
        log.info { "Successfully inserted delegate request for ${data.deviceInfo.name.ifEmpty { reqId }} on $pinOrQr from $ip\n ${Gson().toJson(data)}" }
        if (expires == null) {
            log.error { "Expiry is null!" }
        }
        return ResponseEntity.ok(RequestCreatedResponse(
            expiry = expires?.toEpochSeconds() ?: 0,
            sessionId = reqId
        ))
    }


    @GetMapping(value = ["/delegate/request/pending/{pin}"])
    @RequiresAuthentication(Mode.Strict)
    fun getPendingRequestOnPIN(@PathVariable pin: String): ResponseEntity<DelegatedRequestData> {
        val data = DelegatedAuthenticationTable.executeGetDelegatePendingRequestBy(pin)
        if (data == null) {
            return ResponseEntity.notFound().build()
        }
        log.info { "Returning ${Gson().toJson(data)}" }
        return ResponseEntity.ok(data);
    }



    @PostMapping(value = ["/delegate/permit/request/{session}/{pin}"])
    @RequiresAuthentication(Mode.Strict)
    fun permitDelegationRequest(@RequestBody permitData: PermitRequestData, @PathVariable session: String, @PathVariable pin: String): ResponseEntity<String> {
        val success = executeWithStatus {
            DelegatedAuthenticationTable.update({
                (DelegatedAuthenticationTable.requesterId eq session) and
                        (DelegatedAuthenticationTable.pin eq pin)
            }) {
                it[permitted] = true
            }
        }
        return if (success) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

}