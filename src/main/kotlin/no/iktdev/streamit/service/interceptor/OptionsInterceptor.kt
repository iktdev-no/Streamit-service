package no.iktdev.streamit.service.interceptor

import no.iktdev.streamit.shared.Env
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class OptionsInterceptor : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if (request.method.equals("OPTIONS", ignoreCase = true)) {
            response.status = HttpServletResponse.SC_OK
            response.setHeader("Access-Control-Allow-Origin", Env.getAllowedOrigins().joinToString(",")) // eller dine tillatte origins
            response.setHeader("Access-Control-Allow-Methods", Env.getMethods().joinToString(","))
            response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers") ?: "*")
            response.setHeader("Access-Control-Allow-Credentials", Env.getAllowCredentials().toString())

            return false // Stopper videre håndtering, svarer direkte
        }
        return true // Fortsett normalt for andre metoder
    }
}