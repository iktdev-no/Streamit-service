package no.iktdev.streamit.shared

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import mu.KotlinLogging
import no.iktdev.streamit.shared.classes.remote.RequestDeviceInfo
import no.iktdev.streamit.shared.database.AccessTokenTable
import no.iktdev.streamit.shared.database.queries.executeInsertOrUpdate
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

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
        return if (decoded.expiresAtAsInstant != null)
            decoded.expiresAtAsInstant.isBefore(Instant.now())
        else true
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
            AccessTokenTable.executeInsertOrUpdate(deviceId, token)
        } catch (e: Exception) {
            return null
        }
    }

    class MissingConfigurationException(message: String): Exception(message)
}