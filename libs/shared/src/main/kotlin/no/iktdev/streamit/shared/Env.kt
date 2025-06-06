package no.iktdev.streamit.shared

import java.io.File
import java.time.LocalDateTime

object Env {
    var address: String? = System.getenv("DATABASE_ADDRESS") ?: "192.168.2.250" // "streamit-db"
    var port: String? = System.getenv("DATABASE_PORT") ?: "8082" //"3306"
    var username: String = System.getenv("DATABASE_USERNAME") ?: "streamit"
    var password: String = System.getenv("DATABASE_PASSWORD") ?: "shFZ27eL2x2NoxyEDBMfDWkvFO"
    var database: String = System.getenv("DATABASE_USE") ?: "streamit"

    var content: File? = if (!System.getenv("CONTENT_DIRECTORY").isNullOrEmpty()) File(System.getenv("CONTENT_DIRECTORY")) else null

    var frshness: Long = System.getenv("CONTENT_FRESH_DAYS")?.toLong() ?: 5
    var serieAgeCap: String = System.getenv("SERIE_AGE") ?: "30d"
    var continueWatch: Int = System.getenv("CONTENT_CONTINUE")?.toInt() ?: 10
    var jwtSecret: String? = System.getenv("JWT_SECRET") ?: "eO5zESo8livHiDWxwn+J5U7h5cAZPgWZr4JymG94zB0="
    var jwtExpiry: String? = System.getenv("JWT_EXPIRY") ?: "0d"

    var firebaseServiceFile: String? = System.getenv("FCM_FILE") ?: null


    fun getSerieCutoff(): LocalDateTime {
        val time = serieAgeCap
        var recentAdded = LocalDateTime.now()
        recentAdded = when {
            time.contains("d") -> {
                val days = time.trim('d').toLong()
                recentAdded.minusDays(days)
            }
            time.contains("m") -> {
                val months = time.trim('m').toLong()
                recentAdded.minusMonths(months)
            }
            time.contains("y") -> {
                val years = time.trim('y').toLong()
                recentAdded.minusYears(years)
            }
            else -> {
                recentAdded.minusDays(Env.frshness*3)
            }
        }
        return recentAdded
    }

    fun getExpiry(ttl: String = "0d"): LocalDateTime {
        val current = LocalDateTime.now()
        when {
            ttl.contains("min") -> {
                val min = ttl.substringBefore("min").toLong()
                current.plusMinutes(min)
            }
            ttl.contains("h") -> {
                val h = ttl.substringBefore("h").toLong()
                current.plusHours(h)
            }
            ttl.contains("d") -> ttl.substringBefore("d").toLong().also {
                current.plusDays(it)
            }
            ttl.contains("m") -> ttl.substringBefore("m").toLong().also {
                current.plusMonths(it)
            }
            ttl.contains("y") -> ttl.substringBefore("y").toLong().also {
                current.plusYears(it)
            }

            else -> current.plusDays(30)
        }
        return current
    }

}