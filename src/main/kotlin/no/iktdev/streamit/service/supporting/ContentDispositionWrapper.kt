package no.iktdev.streamit.service.supporting

import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponseWrapper
import org.springframework.http.ContentDisposition
import java.nio.charset.StandardCharsets

class ContentDispositionWrapper(response: HttpServletResponse) : HttpServletResponseWrapper(response) {

    override fun setHeader(name: String, value: String?) {
        super.setHeader(name, sanitizeHeader(name, value))
    }

    override fun addHeader(name: String, value: String?) {
        super.addHeader(name, sanitizeHeader(name, value))
    }

    private fun sanitizeHeader(name: String, value: String?): String? {
        // Hvis det er Content-Disposition og verdien inneholder et ulovlig tegn (utenfor ISO-8859-1)
        if (name.equals("Content-Disposition", ignoreCase = true) && value != null && !isIso8859_1(value)) {
            // Prøv å trekke ut det originale filnavnet fra "filename="
            val filenameRegex = """filename\s*=\s*"([^"]+)"""".toRegex()
            val match = filenameRegex.find(value)
            
            if (match != null) {
                val originalFilename = match.groupValues[1]
                
                // Generer en RFC 5987-kompatibel header ved hjelp av Springs innebygde verktøy
                return ContentDisposition.attachment()
                    .filename(originalFilename, StandardCharsets.UTF_8)
                    .build()
                    .toString()
            }
        }
        return value
    }

    // Sjekker om strengen inneholder tegn som Tomcat vil kaste feil på
    private fun isIso8859_1(text: String): Boolean {
        return text.chars().allMatch { it in 0..255 }
    }
}