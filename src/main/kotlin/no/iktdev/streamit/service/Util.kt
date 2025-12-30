package no.iktdev.streamit.service

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import no.iktdev.streamit.service.interceptor.BaseAuthorizationInterceptor.MissingAccessModeException
import java.io.File
import java.math.BigInteger
import java.nio.file.Paths
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

fun HttpServletRequest?.getRequestersIp(): String? {
    this ?: return null
    val xforwardedIp: String? = this.getHeader("X-Forwarded-For")
    return if (xforwardedIp.isNullOrEmpty()) {
        this.remoteAddr
    } else xforwardedIp
}

fun HttpServletRequest?.getAuthorization(): String? {
    val bearerToken = this?.getHeader("Authorization")
    return if (bearerToken?.contains("Bearer", true) == true) {
        bearerToken.substring(bearerToken.indexOf(" ")+1)
    } else {
        this?.getParameter("token")
    }
}

fun HttpServletRequest.doesEndpointRequireAuthorization(): Boolean {
    val uri = this.requestURI;
    return when (val mode = this.getAttribute("internalAccessMode")) {
        "open" -> false
        "secure" -> true
        else -> {
            throw MissingAccessModeException("Intercepted requirest does not contain requires access mode. Expected is secure or open, provided was: $mode. Access was attempted from $uri")
        }
    }
}

fun toSHA256Hash(input: String): String {
    val bytes = input.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)

    val result = StringBuilder()
    for (byte in digest) {
        result.append(String.format("%02x", byte))
    }

    return result.toString()
}

fun String.toMD5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(this.toByteArray()))
        .toString(16)
        .padStart(32, '0')
}

fun File.with(vararg path: String): File {
    return Paths.get(this.path, *path).toFile()
}

fun File.getOnlyFiles(): List<File> {
    return this.walk().filter { it.isFile }.toList()
}

fun isDebug(): Boolean {
    return Env.mode == "debug"
}

fun debugLog(message: String) {
    if (Env.mode == "debug") {
        val stackTrace = Thread.currentThread().stackTrace
        val caller = stackTrace.getOrNull(3)
        val origin = if (caller != null) "${caller.className}.${caller.methodName}:${caller.lineNumber}" else "UnknownOrigin"
        KotlinLogging.logger {}.info { "[DEBUG][$origin] $message" }
    }
}

fun LocalDateTime.asZoned(): Instant {
    val zone = ZoneOffset.systemDefault().rules.getOffset(Instant.now());
    return this.toInstant(zone)
}