package no.iktdev.streamit.service.services

import mu.KotlinLogging
import no.iktdev.streamit.service.db.tables.content.TitleTable
import no.iktdev.streamit.service.db.tables.util.withTransaction
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Service
import java.text.Normalizer

@Service
class TitleSearchService {
    val log = KotlinLogging.logger {}


    // 1. Normalisering (Bevarer og dekomponerer tegn)
    @VisibleForTesting
    internal fun String.normalize(): String {
        // Normaliserer til NFKD-form (skiller base-karakterer fra diakritiske tegn)
        // Dette gjør at f.eks. 'Á' blir 'A' + '´', noe som er mye lettere å kontrollere
        val normalized = Normalizer.normalize(this, Normalizer.Form.NFKD)

        // Fjerner diakritiske tegn (aksenter osv.) hvis ønskelig,
        // men bevarer selve bokstavene
        return normalized.replace("\\p{M}".toRegex(), "")
    }

    fun findMasterBySanitized(raw: String, database: Database? = null, onError: ((Exception) -> Unit)? = null): String? = withTransaction(database, onError) {

        if (raw.isBlank()) {
            return@withTransaction null
        }

        val result = TitleTable
            .selectAll()
            .firstOrNull { row ->
                val masterSan = row[TitleTable.masterTitle]
                val altSan = row[TitleTable.alternativeTitle]

                masterSan == raw || altSan == raw
            }
            ?.get(TitleTable.masterTitle)

        log.info("Title search using $raw gave $result")

        result
    }

    fun batchSearch(names: List<String>, database: Database? = null, onError: ((Exception) -> Unit)? = null): String? = withTransaction(database, onError) {
        if (names.isEmpty()) return@withTransaction null

        // Hent rader som matcher noen av navnene (enten som master eller alt)
        val rows = TitleTable
            .selectAll()
            .map { row ->
                val master = row[TitleTable.masterTitle]
                val alt = row[TitleTable.alternativeTitle]
                master to alt
            }

        // Tell opp treff per mastertittel basert på sanitering
        val matchCounts = mutableMapOf<String, Int>()

        for ((master, alt) in rows) {

            // Sjekk om noen av våre søkenavn matcher denne radens master eller alt
            for (sanName in names) {
                if (master == sanName || alt == sanName) {
                    matchCounts[master] = matchCounts.getOrDefault(master, 0) + 1
                }
            }
        }

        // Finn den mastertittelen som fikk flest treff
        val bestMatch = matchCounts.maxByOrNull { it.value }?.key

        log.info("Batch search for $names gave best match '$bestMatch' with counts: $matchCounts")

        bestMatch
    }


}