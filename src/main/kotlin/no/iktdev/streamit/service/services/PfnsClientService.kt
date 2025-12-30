package no.iktdev.streamit.service.services

import mu.KotlinLogging
import no.iktdev.streamit.service.Env
import no.iktdev.streamit.service.dto.CapabilitiesObject
import no.iktdev.streamit.service.dto.pfns.PfnsRemoteServerObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class PfnsClientService(
    @Autowired val pfnsRestTemplate: RestTemplate
) {
    val log = KotlinLogging.logger {}
    lateinit var pfnsToken: String

    private fun isPfnsAvailable(): Boolean {
        val _pfnsToken = Env.pfnsApiToken
        if (_pfnsToken.isNullOrBlank()) {
            log.warn { "PFNS API Token is not set.\n\tRemote configuration will be unavailable\n\tPlease create a api token at https://pfns.iktdev.no\n" }
            return false
        }
        pfnsToken = _pfnsToken

        val header = HttpHeaders().apply {
            setBearerAuth(pfnsToken)
        }
        val entity = HttpEntity<String>(null, header)

        val response = try {
            pfnsRestTemplate.exchange(
                "/api/token/validate",
                HttpMethod.POST,
                entity,
                String::class.java
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.METHOD_FAILURE).body("Error validating PFNS API Token: ${e.message}")
        }

        return if (response.statusCode.is2xxSuccessful) {
            log.info { "PFNS API Token is valid." }
            true
        } else if (response.statusCode == HttpStatus.UNAUTHORIZED) {
            log.error { "Invalid PFNS API Token provided. Please check your token." }
            false
        } else {
            log.error { "Failed to validate PFNS API Token: ${response.statusCode} - ${response.body}" }
            false
        }
    }

    init {
        CapabilitiesObject.remoteConfigurationAvailable = isPfnsAvailable()
    }

    private fun isAvailable(): Boolean {
        return CapabilitiesObject.remoteConfigurationAvailable
    }

    fun sendServerConfiguration(data: PfnsRemoteServerObject) {
        if (!isAvailable()) {
            log.warn { "PFNS is not available. Cannot send server configuration." }
            return
        }

        val header = HttpHeaders().apply {
            setBearerAuth(pfnsToken)
        }
        val entity = HttpEntity(data, header)

        val response = try {
            pfnsRestTemplate.exchange(
                "/api/fcm/config/server",
                HttpMethod.POST,
                entity,
                String::class.java
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (response == null) {
            log.error { "Failed to send server configuration due to an exception." }
            return
        }
        else if (response.statusCode.is2xxSuccessful) {
            log.info { "PFNS API Token is valid." }
        } else if (response.statusCode == org.springframework.http.HttpStatus.UNAUTHORIZED) {
            log.error { "Invalid PFNS API Token provided. Please check your token." }
        } else {
            log.error { "Failed to validate PFNS API Token: ${response.statusCode} - ${response.body}" }
        }
    }

}