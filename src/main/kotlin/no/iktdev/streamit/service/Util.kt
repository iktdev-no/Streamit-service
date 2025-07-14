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
    val header = this?.getHeader("Authorization")
    return if (header.isNullOrBlank()) this?.getParameter("token") else header
}