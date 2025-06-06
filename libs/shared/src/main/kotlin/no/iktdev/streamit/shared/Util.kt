package no.iktdev.streamit.shared

import java.io.File
import java.nio.file.Paths
import java.security.MessageDigest
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

fun File.with(vararg path: String): File {
    return Paths.get(this.path, *path).toFile()
}