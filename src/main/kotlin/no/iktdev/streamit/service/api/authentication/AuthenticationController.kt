package no.iktdev.streamit.service.api.authentication

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.streamit.library.db.*
import no.iktdev.streamit.library.db.tables.authentication.DelegatedAuthenticationTable
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.getRequestersIp
import no.iktdev.streamit.shared.Authentication
import no.iktdev.streamit.shared.Mode
import no.iktdev.streamit.shared.RequiresAuthentication
import no.iktdev.streamit.shared.classes.remote.*
import no.iktdev.streamit.shared.database.queries.executeGetDelegatePendingRequestBy
import no.iktdev.streamit.shared.database.queries.executeInsertAndGetId
import no.iktdev.streamit.shared.debugLog
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import javax.servlet.http.HttpServletRequest

@ApiRestController
@RequestMapping("/api/auth")
class AuthenticationController() {
    val auth = Authentication()
    val log = KotlinLogging.logger {}


    @RequiresAuthentication(Mode.Strict)
    @GetMapping(value = ["/validate"])
    fun validateToken(request: HttpServletRequest? = null): ResponseEntity<Boolean?> {
        val token = request?.getHeader("Authorization") ?: run {
            debugLog("No Authorization header found in request")
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(false)
        }
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
    fun createJWT(@RequestBody deviceInfo: RequestDeviceInfo): String {
        return auth.createJwt(deviceInfo)
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
        val success = executeWithStatus(onError = { e ->
            log.error { "Failed to insert delegation request for ${data.deviceInfo.name.ifEmpty { reqId }} on $pinOrQr from $ip\n ${Gson().toJson(data)}" }
            val isCuasedByDuplication = (e.isExposedSqlException() && (e as ExposedSQLException).isCausedByDuplicateError())
            if (isCuasedByDuplication) {
                log.error { "(Confirmed) Duplicate key violation for request ID: $reqId. This might be a retry."}
            } else if (e.message.orEmpty().contains("Duplicate entry")) {
                log.warn { "Duplicate key violation for request ID: $reqId. This might be a retry." }
            } else {
                e.printStackTrace()
            }
        }) {
            insertedId = DelegatedAuthenticationTable.executeInsertAndGetId(
                pin = data.pin,
                requestId = reqId,
                deviceInfo = Gson().toJson(data.deviceInfo),
                method = pinOrQr,
                ip = ip
            ).value
        }
        if (!success) {
            return ResponseEntity.unprocessableEntity().build()
        }
        val expires = withTransaction {
            DelegatedAuthenticationTable.selectAll()
                .where {
                DelegatedAuthenticationTable.id eq insertedId
            }.map { it[DelegatedAuthenticationTable.expires] }.firstOrNull()
        }
        log.info { "Successfully inserted delegate request for requestId: $reqId with data ${data.deviceInfo.name.ifEmpty { reqId }} on $pinOrQr from $ip\n ${Gson().toJson(data)}" }
        if (expires == null) {
            log.error { "Expiry is null!" }
        }
        return ResponseEntity.ok(RequestCreatedResponse(
            expiry = expires?.toEpochSeconds() ?: 0,
            sessionId = reqId
        ))
    }


    @GetMapping(value = ["/delegate/request/pending/{pin}/info"])
    @RequiresAuthentication(Mode.Strict)
    fun getPendingRequestOnPIN(@PathVariable pin: String): ResponseEntity<DelegatedRequestData> {
        val data = DelegatedAuthenticationTable.executeGetDelegatePendingRequestBy(pin)
        if (data == null) {
            return ResponseEntity.notFound().build()
        }
        log.info { "Returning ${Gson().toJson(data)}" }
        return ResponseEntity.ok(data);
    }

    @GetMapping(value = ["/delegate/request/pending/{pin}/permitted/{session}"])
    @RequiresAuthentication(Mode.None)
    fun getPermittedStatusAndToken(@PathVariable pin: String, @PathVariable session: String, request: HttpServletRequest? = null): ResponseEntity<String> {
        val result = try {
            transaction {
                DelegatedAuthenticationTable.selectAll()
                    .where {
                        (DelegatedAuthenticationTable.pin eq pin) and
                                (DelegatedAuthenticationTable.requesterId eq session)
                    }
                .firstNotNullOfOrNull {
                    InternalDelegatedRequestData(
                        pin = it[DelegatedAuthenticationTable.pin],
                        requesterId = it[DelegatedAuthenticationTable.requesterId],
                        created = it[DelegatedAuthenticationTable.created],
                        expires = it[DelegatedAuthenticationTable.expires],
                        consumed = it[DelegatedAuthenticationTable.consumed],
                        permitted = it[DelegatedAuthenticationTable.permitted],
                        ipaddress = it[DelegatedAuthenticationTable.ipaddress]
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity.internalServerError().build()
        }

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
        } else {
            result.let {  consumable ->
                transaction {
                    DelegatedAuthenticationTable.update({
                        (DelegatedAuthenticationTable.requesterId eq consumable.requesterId) and
                                (DelegatedAuthenticationTable.pin eq consumable.pin)
                    }) {
                        it[consumed] = true
                    }
                }
            }
            ResponseEntity.ok(auth.createJwt(null))
        }
    }

    @PostMapping(value = ["/delegate/request/{session}/{pin}/permit"])
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