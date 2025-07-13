package no.iktdev.streamit.service.interceptor

import mu.KotlinLogging
import no.iktdev.streamit.shared.Env
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class OptionsInterceptor : HandlerInterceptor {
    val log = KotlinLogging.logger {}

    init {
        log.info { "Loading ${this.javaClass.simpleName}" }
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        log.info("${this.javaClass.simpleName} triggered on ${request.method}")

        if (request.method.equals("OPTIONS", ignoreCase = true)) {
            log.info("Identified preflight")

            val allowedOrigins = Env.getAllowedOrigins().joinToString(",")
            val allowedMethods = Env.getMethods().joinToString(",")
            val allowedHeaders = request.getHeader("Access-Control-Request-Headers") ?: "*"
            val allowCredentials = Env.getAllowCredentials().toString()

            response.status = HttpServletResponse.SC_OK
            response.setHeader("Access-Control-Allow-Origin", allowedOrigins)
            response.setHeader("Access-Control-Allow-Methods", allowedMethods)
            response.setHeader("Access-Control-Allow-Headers", allowedHeaders)
            response.setHeader("Access-Control-Allow-Credentials", allowCredentials)

            // Logg respons-headers
            log.info("Response headers for OPTIONS:")
            log.info("Access-Control-Allow-Origin: $allowedOrigins")
            log.info("Access-Control-Allow-Methods: $allowedMethods")
            log.info("Access-Control-Allow-Headers: $allowedHeaders")
            log.info("Access-Control-Allow-Credentials: $allowCredentials")
            log.info("Response status: ${response.status}")

            return false // Stopper videre håndtering, svarer direkte
        }
        return true // Fortsett normalt for andre metoder
    }
}