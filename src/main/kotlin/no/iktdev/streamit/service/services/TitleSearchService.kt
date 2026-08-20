package no.iktdev.streamit.service.services

import mu.KotlinLogging
import no.iktdev.streamit.service.db.tables.content.TitleTable
import no.iktdev.streamit.service.db.tables.util.withTransaction
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Service
import java.text.Normalizer

@Service
class TitleSearchService {
    val log = KotlinLogging.logger {}

    @VisibleForTesting
    internal fun String.normalize(): String {
        val normalized = Normalizer.normalize(this, Normalizer.Form.NFKD)
        return normalized.replace("\\p{M}".toRegex(), "").lowercase()
    }

    fun findMasterBySanitized(raw: String, database: Database? = null, onError: ((Exception) -> Unit)? = null): String? = withTransaction(database, onError) {
        if (raw.isBlank()) {
            return@withTransaction null
        }

        val normalizedSearch = raw.normalize()

        val result = TitleTable
            .selectAll()
            .firstOrNull { row ->
                val masterSan = row[TitleTable.masterTitle].normalize()
                val altSan = row[TitleTable.alternativeTitle].normalize()

                masterSan == normalizedSearch || altSan == normalizedSearch
            }
            ?.get(TitleTable.masterTitle)

        log.info("Title search using '$raw' (normalized: '$normalizedSearch') gave $result")

        result
    }

    fun batchSearch(names: List<String>, database: Database? = null, onError: ((Exception) -> Unit)? = null): String? = withTransaction(database, onError) {
        if (names.isEmpty()) return@withTransaction null

        val normalizedNames = names.map { it.normalize() }.toSet()

        val rows = TitleTable
            .selectAll()
            .map { row ->
                val master = row[TitleTable.masterTitle]
                val alt = row[TitleTable.alternativeTitle]
                master to alt.normalize()
                // Vi beholder original 'master' som nøkkel for å returnere den riktige tittelen,
                // men sammenligner med en normalisert versjon av master og alt.
            }

        // Tell opp treff per mastertittel basert på normalisert sanitering
        val matchCounts = mutableMapOf<String, Int>()

        for ((master, alt) in rows) {
            val normalizedMaster = master.normalize()

            for (sanName in normalizedNames) {
                if (normalizedMaster == sanName || alt == sanName) {
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