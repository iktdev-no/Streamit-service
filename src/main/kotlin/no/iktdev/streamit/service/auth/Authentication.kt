package no.iktdev.streamit.service.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.streamit.service.Env
import no.iktdev.streamit.service.asZoned
import no.iktdev.streamit.service.db.tables.pfns.PersistentTokenTable
import no.iktdev.streamit.service.db.tables.pfns.TokenTable
import no.iktdev.streamit.service.dto.auth.MediaScopedAuthRequest
import no.iktdev.streamit.service.dto.auth.RequestDeviceInfo
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.math.min

open class Authentication {
    private val log = KotlinLogging.logger {}

    companion object {
        fun algorithm(): Algorithm {
            return Algorithm.HMAC256(Env.jwtSecret) ?: throw MissingConfigurationException("HS256 JWT secret is not provided correctly, clear environment variable to use default...")
        }
        val issuer = "Streamit services"
    }

    private fun hasBearer(jwt: String): Boolean {
        return jwt.contains("Bearer", true)
    }

    fun decode(jwt: String): DecodedJWT? {
        val strippedBearer = if (hasBearer(jwt)) jwt.substring(jwt.indexOf(" ")+1) else jwt

        val verifier = JWT.require(algorithm()).withIssuer(issuer).build()
        return try {
            verifier.verify(strippedBearer)
        } catch (e: JWTVerificationException) {
            null
        }
    }

    fun isTokenValid(token: String? = null): Boolean {
        if (token.isNullOrEmpty()) {
            log.error { "No Authorization token found!" }
            return false
        }
        val decoded = decode(token) ?: return false
        return decoded.expiresAtAsInstant?.isAfter(Instant.now()) ?: true
    }

    enum class TokenType {
        Device,
        Cast
    }

    fun createJwt(deviceInfo: RequestDeviceInfo? = null, ttl: String? = null): String? {
        val deviceId = deviceInfo?.toRequestId() ?: UUID.randomUUID().toString()
        val zone = ZoneOffset.systemDefault().rules.getOffset(Instant.now())
        val deviceMap = if (deviceInfo != null) {
            mapOf(
                "id" to deviceId,
                "name" to deviceInfo.name,
                "model" to deviceInfo.model,
                "deviceManufacturer" to deviceInfo.manufacturer,
                "clientOrOsVersion" to deviceInfo.clientOrOsVersion,
                "clientOrOsPlatform" to deviceInfo.clientOrOsPlatform
            )
        } else emptyMap()

        val builder = JWT.create()
            .withIssuer(issuer)
            .withIssuedAt(Date.from(Instant.now()))
            .withSubject("Authorization for A.O.I.")
            .withPayload(mapOf(
                "type" to TokenType.Device.name,
                "scope" to userDefaultScope(),
                "device" to deviceMap
            ))

        val setTtl = if (!ttl.isNullOrBlank()) {
            ttl
        } else if (!Env.jwtExpiry.isNullOrBlank()) {
            Env.jwtExpiry
        } else {
            null
        }

        val expiry = setTtl?.let { Env.getExpiry(it) }

        if (expiry != null) {
            builder.withExpiresAt(expiry.toInstant(zone))
        } else {
            log.warn { "Access token is created without expiry" }
        }

        val token = builder.sign(algorithm())
        return try {
            PersistentTokenTable.executeInsertOrUpdate(deviceId, token)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /** Token expiry is dynamically calculated based on the number of requested video files.
        This ensures that each playback session has a limited lifespan, reducing the risk of token misuse.
        Especially important in public networks, where tokens could be intercepted.
        Google Cast sessions are also auto-terminated after inactivity, providing an additional layer of protection.
     **/
    fun createMediaScopedJwt(scopeInfo: MediaScopedAuthRequest, tokenType: TokenType, scopes: Map<String, List<String>>): String? {
        val videoFilesRequested = scopes.values.flatten().count { it -> it.startsWith("video/") }
        val maxViewingHours = 12L
        val designatedExpiryTime = min(if (videoFilesRequested <= 1) 3 else 3 * videoFilesRequested, maxViewingHours.toInt())
        val expire = LocalDateTime.now().plusHours(designatedExpiryTime.toLong()).asZoned()

        val builder = JWT.create()
            .withIssuer(issuer)
            .withIssuedAt(LocalDateTime.now().asZoned())
            .withSubject("Scoped Authorization token")
            .withPayload(mapOf(
                "type" to tokenType.name,
                "scope" to scopes,
                "media" to Gson().toJson(scopeInfo)
            ))
            .withExpiresAt(expire)
        val token =  builder.sign(algorithm())
        return try {
            val success = TokenTable.insertToken(token)
            if (success) token else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    class MissingConfigurationException(message: String): Exception(message)
}