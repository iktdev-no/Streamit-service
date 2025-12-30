package no.iktdev.streamit.service

import mu.KotlinLogging
import no.iktdev.streamit.service.interceptor.GeneralAuthorizationInterceptor
import no.iktdev.streamit.service.interceptor.MediaAuthorizationInterceptor
import no.iktdev.streamit.service.Env
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerTypePredicate
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper


val log = KotlinLogging.logger {}

@SpringBootApplication
class Application {
}

var context: ApplicationContext? = null
fun main(args: Array<String>) {
    context = runApplication<Application>(*args)
}


@Configuration
class InterceptorConfiguration(
    @Autowired val authInterceptor: GeneralAuthorizationInterceptor,
    @Autowired val mediaInterceptor: MediaAuthorizationInterceptor
): WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        super.addInterceptors(registry)
        authInterceptor.let {
            log.info("Adding ${it.javaClass.simpleName}")
            registry.addInterceptor(it)
                .addPathPatterns("/**")
                .excludePathPatterns("/stream/**")
        }
        mediaInterceptor.let {
            log.info("Adding ${it.javaClass.simpleName}")
            registry.addInterceptor(it)
                .addPathPatterns("/stream/**")
        }

    }

    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        super.configurePathMatch(configurer)
        configurer.addPathPrefix("/api", HandlerTypePredicate.forAnnotation(ApiRestController::class.java))
        configurer.addPathPrefix("/stream", HandlerTypePredicate.forAnnotation(ContentRestController::class.java))
        configurer.addPathPrefix("/assets", HandlerTypePredicate.forAnnotation(AssetRestController::class.java))
    }
}

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        val origins = Env.getAllowedOrigins();
        log.info("Allowing origins:\n\t" + origins.joinToString("\n\t"))

        val methods = Env.getMethods()
        log.info("Allowing methods:\n\t" + methods.joinToString("\n\t"))

        registry.addMapping("/**")
            .allowedOrigins(*origins.toTypedArray()) // or specify allowed origins
            .allowedMethods(*methods.toTypedArray())
            .allowCredentials(Env.getAllowCredentials())
    }
}



@Component
@Order(2)
class PathDefiner : Filter {

    private fun setModifiedPath(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val uri = httpRequest.requestURI

        val accessModes = listOf("open", "secure")
        var mode = "unknown"
        var internalPath = uri

        for (access in accessModes) {
            when {
                uri.startsWith("/$access/api") -> {
                    mode = access
                    internalPath = "/api" + uri.removePrefix("/$access/api")
                    break
                }
                uri.startsWith("/$access/stream") -> {
                    mode = access
                    internalPath = "/stream" + uri.removePrefix("/$access/stream")
                    break
                }
                uri.startsWith("/$access/assets") -> {
                    mode = access
                    internalPath = "/assets" + uri.removePrefix("/$access/assets")
                    break
                }
            }
        }

        request.removeAttribute("internalAccessMode")
        request.setAttribute("internalAccessMode", mode)

        val modifiedRequest = object : HttpServletRequestWrapper(httpRequest) {
            override fun getRequestURI(): String {
                return internalPath
            }
        }
        chain.doFilter(modifiedRequest, response)
    }


    private fun setSinglePath(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        request.setAttribute("internalAccessMode", "open")
        chain.doFilter(request, response)
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        if (Env.singleEntryPaths) {
            setSinglePath(request, response, chain)
        } else {
            setModifiedPath(request, response, chain)
        }
    }
}

