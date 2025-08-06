package no.iktdev.streamit.service

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.servlet.http.HttpServletRequest

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