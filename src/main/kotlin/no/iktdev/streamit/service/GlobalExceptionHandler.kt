package no.iktdev.streamit.service

import com.fasterxml.jackson.databind.JsonMappingException
import no.iktdev.streamit.shared.isDebug
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerMapping
import java.io.ByteArrayInputStream
import javax.servlet.FilterChain
import javax.servlet.ReadListener
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import javax.servlet.http.HttpServletResponse


@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleJsonParseError(
        request: HttpServletRequest,
        ex: HttpMessageNotReadableException
    ): ResponseEntity<String> {
        if (isDebug()) {
            val rawJson = request.getAttribute("cachedJsonBody") as? String
            rawJson?.let { logger.error("Failed to parse JSON: $it") }

            val handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE)
            if (handler is HandlerMethod) {
                val controller = handler.beanType.simpleName
                val method = handler.method.name
                logger.error("Error occurred in $controller#$method")
            }

            val cause = ex.cause
            if (cause is JsonMappingException) {
                logger.error("Jackson path: ${cause.pathReference}")
                logger.error("Jackson message: ${cause.originalMessage}")
            }

        }
        return ResponseEntity.badRequest().body("Ugyldig JSON-data mottatt.")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}


class CachedBodyHttpServletRequest(request: HttpServletRequest) : HttpServletRequestWrapper(request) {
    private val cachedBody: ByteArray = request.inputStream.readBytes()

    override fun getInputStream(): ServletInputStream {
        val byteArrayInputStream = ByteArrayInputStream(cachedBody)
        return object : ServletInputStream() {
            override fun read(): Int = byteArrayInputStream.read()
            override fun isFinished(): Boolean = byteArrayInputStream.available() == 0
            override fun isReady(): Boolean = true
            override fun setReadListener(readListener: ReadListener?) {}
        }
    }

    fun getCachedBodyAsString(): String = cachedBody.toString(Charsets.UTF_8)
}

@Component
class JsonLoggingFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val cachedRequest = CachedBodyHttpServletRequest(request)
        val rawJson = cachedRequest.getCachedBodyAsString()
        cachedRequest.setAttribute("cachedJsonBody", rawJson)
        filterChain.doFilter(cachedRequest, response)
    }
}
