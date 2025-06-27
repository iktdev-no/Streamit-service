package no.iktdev.streamit.shared

import mu.KotlinLogging
import java.io.File
import java.nio.file.Paths
import java.security.MessageDigest


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

fun File.getOnlyFiles(): List<File> {
    return this.walk().filter { it.isFile }.toList()
}

fun debugLog(message: String) {
    if (Env.mode == "debug") {
        val stackTrace = Thread.currentThread().stackTrace
        val caller = stackTrace.getOrNull(3)
        val origin = if (caller != null) "${caller.className}.${caller.methodName}:${caller.lineNumber}" else "UnknownOrigin"
        KotlinLogging.logger {}.info { "[DEBUG][$origin] $message" }
    }
}