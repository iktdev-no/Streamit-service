package no.iktdev.streamit.service.interceptor

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import mu.KotlinLogging
import no.iktdev.streamit.service.getAuthorization
import no.iktdev.streamit.service.getRequestersIp
import no.iktdev.streamit.service.services.TokenState
import no.iktdev.streamit.service.services.TokenStateCacheService
import no.iktdev.streamit.shared.Authentication
import no.iktdev.streamit.shared.RequiresAuthentication
import no.iktdev.streamit.shared.Scope
import no.iktdev.streamit.shared.impliedScopes
import no.iktdev.streamit.shared.scopesFromClaims
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import java.util.stream.Collectors
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

abstract class BaseAuthorizationInterceptor: HandlerInterceptor, Authentication() {
    private val log = KotlinLogging.logger {}

    @Autowired lateinit var tokenCacheService: TokenStateCacheService

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val isAuthenticationRequired = try {
            doesEndpointRequireAuthorization(request)
        } catch (e: MissingAccessModeException) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, e.message)
            return false
        }
        if (!isAuthenticationRequired) {
            return true
        }
        val requiredScope = getRequiredTokenScope(request, handler)
        if (requiredScope == Scope.None)
            return true
        val token = request.getAuthorization() ?: run {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is not present in header or as a parameter, when endpoint requires authentication")
            return false
        }
        val tokenState = tokenCacheService.getTokenState(token)
        if (!isTokenValid(token) || tokenState != TokenState.Active) {
            response.status = HttpStatus.UNAUTHORIZED.value()
            when (tokenState) {
                TokenState.NotFound -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token does not exist for this server instance")
                TokenState.Revoked -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is revoked")
                else -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is not valid, as in either tampered or expired")
            }
            return false
        }
        val tokenIsAcceptable = isTokenWithinScope(token, requiredScope)
        if (!tokenIsAcceptable) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token does not have the correct scope configured, and is missing permission for ${requiredScope.name}")
            return false
        }
        return true
    }



    fun doesEndpointRequireAuthorization(request: HttpServletRequest): Boolean {
        val uri = request.requestURI;
        val mode = request.getAttribute("internalAccessMode")
        return when (mode) {
            "open" -> false
            "secure" -> true
            else -> {
                throw MissingAccessModeException("Intercepted requirest does not contain requires access mode. Expected is secure or open, provided was: $mode. Access was attempted from $uri")
            }
        }
    }

    private fun getRequiredTokenScope(request: HttpServletRequest, handler: Any): Scope {
        val validation = try {
            if (handler is HandlerMethod)
                handler.method.getAnnotation(RequiresAuthentication::class.java)
            else {
                log.info { "Handler type: ${handler::class.qualifiedName}" }
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val url = request.requestURL.toString()
            val queryParams = request.queryString
            val body = request.reader.lines().collect(Collectors.joining(System.lineSeparator()))
            log.error { "Error report:\n\tSource:${request.getRequestersIp()}\n\tUrl:$url\n\tQuery params:$queryParams\n\tBody:$body" }
            null
        }
        return validation?.withScope ?: run {
            log.warn { "No handler found on ${request.method} @ ${request.requestURI}" }
            Scope.None
        }
    }

    private fun getScopesFromToken(token: String): List<Scope> {
        val token = decode(token) ?: return emptyList()
        val scopeMap = token.getClaim("scope")?.asMap() ?: return emptyList()
        val scopes = scopeMap.mapValues { (_, value) ->
            if (value is List<*>) {
                value.filterIsInstance<String>() // Trygg filtrering
            } else emptyList()
        }

        val decodedScopes = scopesFromClaims(scopes).values.flatten()
        return decodedScopes + decodedScopes.flatMap { impliedScopes(it) }
    }


    fun isTokenWithinScope(token: String, requiredScope: Scope): Boolean {
        if (requiredScope == Scope.None)
            return true
        val tokenScopes = getScopesFromToken(token)
        return requiredScope in tokenScopes
    }


    class MissingAccessModeException(message: String): Exception(message)
}