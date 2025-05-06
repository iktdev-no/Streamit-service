package no.iktdev.streamit.shared

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import mu.KotlinLogging
import no.iktdev.streamit.shared.classes.Jwt
import no.iktdev.streamit.shared.classes.User
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
        return decoded.expiresAtAsInstant.isBefore(Instant.now())
    }


    fun createJwt(user: User? = null, ttl: String? = null): Jwt {
        val zone = ZoneOffset.systemDefault().rules.getOffset(Instant.now())
        val usermap = user?.let { usr ->
            mapOf(
                "guid" to usr.guid,
                "name" to usr.name,
                "image" to usr.image
            )
        }
        val builder = JWT.create()
            .withIssuer(issuer)
            .withIssuedAt(Date.from(Instant.now()))
            .withSubject("Authorization for A.O.I.")
        usermap?.let { payload ->
            builder.withPayload(mapOf("user" to payload))
        }

        val setTtl = if (user == null) {
            "5min"
        } else if (!ttl.isNullOrBlank()) {
            ttl
        } else Env.jwtExpiry

        val expiry = Env.getExpiry(setTtl ?: "0d")

        builder.withExpiresAt(expiry.toInstant(zone))

        return Jwt(builder.sign(algorithm()))
    }

    class MissingConfigurationException(message: String): Exception(message)
}