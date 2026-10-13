package no.iktdev.streamit.service.controller.api.authentication

import io.swagger.v3.oas.annotations.tags.Tag
import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.doesEndpointRequireAuthorization
import no.iktdev.streamit.service.getAuthorization
import no.iktdev.streamit.service.getRequestersIp
import no.iktdev.streamit.service.services.TokenState
import no.iktdev.streamit.service.services.TokenStateCacheService
import no.iktdev.streamit.service.stores.authentication.IDelegatedAuthenticationStore
import no.iktdev.streamit.service.auth.Authentication
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.auth.castScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import jakarta.servlet.http.HttpServletRequest
import no.iktdev.streamit.service.db.tables.auth.DelegatedAuthenticationTable
import no.iktdev.streamit.service.debugLog
import no.iktdev.streamit.service.model.internal.auth.AuthInitiateRequest
import no.iktdev.streamit.service.model.internal.auth.DelegatedRequestData
import no.iktdev.streamit.service.model.internal.auth.MediaScopedAuthRequest
import no.iktdev.streamit.service.model.internal.auth.PermitRequestData
import no.iktdev.streamit.service.model.internal.auth.RequestCreatedResponse
import no.iktdev.streamit.service.model.internal.auth.RequestDeviceInfo

@ApiRestController
@Tag(name = "Authentication", description = "Device token issuance, validation, and PIN/QR authorization flows")
@RequestMapping("/auth")
class AuthenticationController(
    private val delegatedAuthenticationStore: IDelegatedAuthenticationStore
) {
    @Autowired lateinit var tokenStateCacheService: TokenStateCacheService

    val auth = Authentication()
    val log = KotlinLogging.logger {}


    @RequiresAuthentication(Scope.AuthorizedRead)
    @GetMapping(value = ["/validate"])
    fun validateToken(request: HttpServletRequest? = null): ResponseEntity<Boolean?> {
        val token = request.getAuthorization()  ?: run {
            debugLog("No Authorization header found in request")
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(false)
        }
        val isValid = auth.isTokenValid(token) && tokenStateCacheService.getTokenState(token) == TokenState.Active
        return if (isValid) {
            ResponseEntity.status(200).body(true)
        } else {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false)
        }
    }

    @RequiresAuthentication(Scope.AuthorizationCreate)
    @PostMapping(value = ["/new"])
    fun createJWT(@RequestBody deviceInfo: RequestDeviceInfo): ResponseEntity<String> {
        val token = auth.createJwt(deviceInfo)
        if (token == null) {
            log.error { "Failed to create JWT for device: ${deviceInfo.name}" }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
        return ResponseEntity.ok(token)
    }

    /**
     * Will create a scoped token,
     * Limited lifetime (3h)
     * Only able to play media associated defined base name
     */
    @RequiresAuthentication(Scope.AuthorizationCreate)
    @PostMapping(value = ["/new/cast"])
    fun createScopedCastJwt(@RequestBody scopeInfo: MediaScopedAuthRequest): ResponseEntity<String> {
        val token = auth.createMediaScopedJwt(scopeInfo, Authentication.TokenType.Cast,castScope())
        if (token == null) {
            log.error { "Failed to create scoped token" }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
        return ResponseEntity.ok(token)
    }

    @GetMapping(value = ["/delegate/required"])
    @RequiresAuthentication(Scope.None)
    fun doesRequireDelegate(request: HttpServletRequest): ResponseEntity<Boolean> {
        val doesRequire = request.doesEndpointRequireAuthorization()
        return ResponseEntity.ok(doesRequire)
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
    @RequiresAuthentication(Scope.None)
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
    @RequiresAuthentication(Scope.None)
    fun createQRDelegationRequestSession(@RequestBody data: AuthInitiateRequest, request: HttpServletRequest? = null): ResponseEntity<RequestCreatedResponse> {
        return createDelegationRequestSession(data, DelegatedAuthenticationTable.AuthMethod.QR, request)
    }


    fun createDelegationRequestSession(data: AuthInitiateRequest, pinOrQr: DelegatedAuthenticationTable.AuthMethod, request: HttpServletRequest?): ResponseEntity<RequestCreatedResponse> {
        val ip = request?.getRequestersIp()
        val reqId = data.toRequestId()
        val expires = delegatedAuthenticationStore.createRequest(
            pin = data.pin,
            requesterId = reqId,
            deviceInfo = data.deviceInfo,
            method = pinOrQr,
            ipAddress = ip
        ).getOrElse { error ->
            log.error(error) {
                "Failed to insert delegation request for ${data.deviceInfo.name.ifEmpty { reqId }} on $pinOrQr from $ip"
            }
            return ResponseEntity.unprocessableEntity().build()
        }
        log.info { "Successfully inserted delegate request for requestId: $reqId with data ${data.deviceInfo.name.ifEmpty { reqId }} on $pinOrQr from $ip\n ${Gson().toJson(data)}" }
        return ResponseEntity.ok(
            RequestCreatedResponse(
                expiry = expires,
                sessionId = reqId
            )
        )
    }


    @GetMapping(value = ["/delegate/request/pending/{pin}/info"])
    @RequiresAuthentication(Scope.AuthorizationPermit)
    fun getPendingRequestOnPIN(@PathVariable pin: String): ResponseEntity<DelegatedRequestData> {
        val data = delegatedAuthenticationStore.getPendingRequest(pin)
        if (data == null) {
            return ResponseEntity.notFound().build()
        }
        log.info { "Returning ${Gson().toJson(data)}" }
        return ResponseEntity.ok(data);
    }

    @GetMapping(value = ["/delegate/request/pending/{pin}/permitted/{session}"])
    @RequiresAuthentication(Scope.None)
    fun getPermittedStatusAndToken(@PathVariable pin: String, @PathVariable session: String, request: HttpServletRequest? = null): ResponseEntity<String> {
        val result = delegatedAuthenticationStore.getRequestStatus(pin, session)
        if (result == null) {
            return ResponseEntity.notFound().build()
        }

        if (request.getRequestersIp() != result.ipaddress) {
            return ResponseEntity.status(409).build()
        }

        log.info { "Consuming authorization on pin: ${result.pin} requested by ${request.getRequestersIp()}" }


        return if (result.expires < LocalDateTime.now() || result.consumed) {
            if (result.consumed) {
                log.info { "Authorization is already consumed" }
            } else {
                log.info { "Authorization is expired.." }
            }
            ResponseEntity.status(HttpStatus.GONE).body(null)
        } else if (!result.permitted) {
            log.info { "Authorization needs to be granted.." }
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)
        } else if (!delegatedAuthenticationStore.consumeIfPermitted(pin, session, request.getRequestersIp())) {
            ResponseEntity.status(HttpStatus.GONE).body(null)
        } else {
            ResponseEntity.ok(auth.createJwt(null))
        }
    }

    @PostMapping(value = ["/delegate/request/{session}/{pin}/permit"])
    @RequiresAuthentication(Scope.AuthorizationPermit)
    fun permitDelegationRequest(@RequestBody permitData: PermitRequestData, @PathVariable session: String, @PathVariable pin: String): ResponseEntity<String> {
        val success = delegatedAuthenticationStore.permit(pin, session)
        return if (success) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

}
