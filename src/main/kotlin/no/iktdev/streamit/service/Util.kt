package no.iktdev.streamit.service

import javax.servlet.http.HttpServletRequest

fun HttpServletRequest?.getRequestersIp(): String? {
    this ?: return null
    val xforwardedIp: String? = this.getHeader("X-Forwarded-For")
    return if (xforwardedIp.isNullOrEmpty()) {
        this.remoteAddr
    } else xforwardedIp
}

fun HttpServletRequest?.getAuthorization(): String? {
    return this?.getHeader("Authorization")
}