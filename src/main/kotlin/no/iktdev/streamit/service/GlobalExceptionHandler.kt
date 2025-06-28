package no.iktdev.streamit.service

import com.fasterxml.jackson.databind.JsonMappingException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest


@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleJsonParseError(ex: HttpMessageNotReadableException, request: WebRequest?): ResponseEntity<String?> {
        val cause = ex.cause
        if (cause is JsonMappingException) {
            val path = cause.pathReference
            val originalMessage = cause.originalMessage
            log.error("JSON parse error at {}: {}", path, originalMessage)
        } else {
            log.error("Unhandled JSON parse error: {}", ex.message)
        }
        return ResponseEntity.badRequest().body<String?>("Ugyldig JSON-data mottatt.")
    }
}