package no.iktdev.streamit.service.interceptor

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.streamit.service.getAuthorization
import no.iktdev.streamit.service.getRequestersIp
import no.iktdev.streamit.service.services.TokenState
import no.iktdev.streamit.shared.RequiresAuthentication
import no.iktdev.streamit.shared.Scope
import no.iktdev.streamit.shared.classes.remote.MediaScopedAuthRequest
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import java.net.URLDecoder
import java.util.stream.Collectors
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Order(1)
class MediaAuthorizationInterceptor: BaseAuthorizationInterceptor() {
    val log = KotlinLogging.logger {}

    init {
        log.info { "Loading ${this.javaClass.simpleName}" }
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val allow = super.preHandle(request, response, handler)
        if (!allow)
            return false // It has already failed preliminary checks

        val token = request.getAuthorization()?.let { decode(it) }
        if (token != null) {
            val tokenType = token.getClaim("type").asString()
            if (tokenType == TokenType.Device.name) {
                return true // Slipp rett igjennom
            }

            val path = request.requestURI.let { it -> URLDecoder.decode(it, "UTF-8") }.substringAfter("/media/")
            if (tokenType == TokenType.Cast.name) {
                val mediaJson = token.getClaim("media").asString()
                val scopeInfo = Gson().fromJson(mediaJson, MediaScopedAuthRequest::class.java)

                val tillatteMedia = scopeInfo.media

                if (tillatteMedia.any {it -> it == path}) {
                    return true
                }

                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied")
                return false
            }

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token type")
            return false
        }

        return true
    }

}