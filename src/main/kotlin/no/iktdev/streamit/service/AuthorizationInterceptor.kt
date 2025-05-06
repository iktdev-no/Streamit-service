package no.iktdev.streamit.service

import mu.KotlinLogging
import no.iktdev.streamit.shared.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import java.util.stream.Collectors
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class AuthorizationInterceptor: HandlerInterceptor, Authentication() {
    val log = KotlinLogging.logger {}

    init {
        log.info { "Loading ${this.javaClass.simpleName}" }
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val uri = request.requestURI;
        val mode = if (uri.startsWith("/api")) uri.substringAfter("/api")
            else if (uri.startsWith("/stream")) uri.substringAfter("/stream")
        else run {
            response.status = HttpStatus.UNPROCESSABLE_ENTITY.value()
            log.warn { "Got no clue with what to do with $uri, does not match stream or api" }
            return false
        }
        if (mode.startsWith("/open")) {
            return true
        } else {
            val mode = getAuthenticationDefined(request, handler)
            if (mode == Mode.None) return true
            val token = request.getAuthorization() ?: run {
                response.status = HttpStatus.UNAUTHORIZED.value()
                return false
            }
            if (!isTokenValid(token)) {
                response.status = HttpStatus.BAD_REQUEST.value()
                return false
            }
            return true
        }
    }

    fun getAuthenticationDefined(request: HttpServletRequest, handler: Any): Mode {
        val validation = try {
            if (handler is HandlerMethod)
                handler.method.getAnnotation(RequiresAuthentication::class.java)
            else null
        } catch (e: Exception) {
            e.printStackTrace()
            val url = request.requestURL.toString()
            val queryParams = request.queryString
            val body = request.reader.lines().collect(Collectors.joining(System.lineSeparator()))
            log.error { "Error report:\n\tSource:${request.getRequestersIp()}\n\tUrl:$url\n\tQuery params:$queryParams\n\tBody:$body" }
            null
        }
        return validation?.mode ?: kotlin.run {
            log.warn { "No handler found on ${request.method} @ ${request.requestURI}" }
            Mode.None
        }
    }


}