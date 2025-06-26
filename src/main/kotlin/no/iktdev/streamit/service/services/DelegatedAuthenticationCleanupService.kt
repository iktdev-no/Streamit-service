package no.iktdev.streamit.service.services

import mu.KotlinLogging
import no.iktdev.streamit.library.db.tables.authentication.DelegatedAuthenticationTable
import no.iktdev.streamit.shared.classes.remote.InternalDelegatedRequestData
import no.iktdev.streamit.shared.getRequestersIp
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.and

@EnableScheduling
@Service
class DelegatedAuthenticationCleanupService {
    val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 0 0 * * ?") // Kjør ved midnatt
    fun resetUsage() {
        try {
            val start = System.currentTimeMillis()
            val deletedRows = transaction {
                DelegatedAuthenticationTable.deleteWhere {
                    (DelegatedAuthenticationTable.expires less LocalDateTime.now()) and
                            (DelegatedAuthenticationTable.consumed eq true)
                }

            }
            val elapsed = System.currentTimeMillis() - start
            log.info { "Cleared $deletedRows row(s) in $elapsed ms" }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}