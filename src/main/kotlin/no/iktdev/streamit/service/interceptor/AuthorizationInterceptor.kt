package no.iktdev.streamit.service.interceptor

import mu.KotlinLogging
import no.iktdev.streamit.service.getAuthorization
import no.iktdev.streamit.service.getRequestersIp
import no.iktdev.streamit.shared.Authentication
import no.iktdev.streamit.shared.Mode
import no.iktdev.streamit.shared.RequiresAuthentication
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import java.util.stream.Collectors
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Order(1)
class AuthorizationInterceptor: HandlerInterceptor, Authentication() {
    val log = KotlinLogging.logger {}

    init {
        log.info { "Loading ${this.javaClass.simpleName}" }
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val uri = request.requestURI;
        val mode = request.getAttribute("internalAccessMode")
        when (mode) {
            "open" -> return true
            "secure" -> run {
                val mode = getAuthenticationDefined(request, handler)
                if (mode == Mode.None) return true
                val token = request.getAuthorization() ?: run {
                    response.status = HttpStatus.UNAUTHORIZED.value()
                    return false
                }
                if (!isTokenValid(token)) {
                    response.status = HttpStatus.UNAUTHORIZED.value()
                    return false
                }
                return true
            }
            else -> run {
                response.status = HttpStatus.UNPROCESSABLE_ENTITY.value()
                log.warn { "Got no clue with what to do with $uri, does not match stream or api" }
                return false
            }
        }
    }

    fun getAuthenticationDefined(request: HttpServletRequest, handler: Any): Mode {
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
        return validation?.mode ?: run {
            log.warn { "No handler found on ${request.method} @ ${request.requestURI}" }
            Mode.None
        }
    }


}