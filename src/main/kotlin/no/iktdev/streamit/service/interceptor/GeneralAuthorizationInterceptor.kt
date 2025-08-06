package no.iktdev.streamit.service.interceptor

import mu.KotlinLogging
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Order(1)
class GeneralAuthorizationInterceptor: BaseAuthorizationInterceptor() {
    val log = KotlinLogging.logger {}

    init {
        log.info { "Loading ${this.javaClass.simpleName}" }
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val allow = super.preHandle(request, response, handler)
        return allow
    }

}