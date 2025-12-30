package no.iktdev.streamit.service.services

import mu.KotlinLogging
import no.iktdev.streamit.service.db.tables.auth.DelegatedAuthenticationTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

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