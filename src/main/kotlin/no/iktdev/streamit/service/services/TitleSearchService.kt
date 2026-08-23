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

        val rows = TitleTable.selectAll().map { row ->
            row[TitleTable.masterTitle] to row[TitleTable.alternativeTitle]
        }

        // Tell opp treff per mastertittel
        val matchCounts = mutableMapOf<String, Int>()

        for ((master, alt) in rows) {
            val normalizedMaster = master.normalize()
            val normalizedAlt = alt.normalize()

            for (sanName in normalizedNames) {
                // Hvis enten mastertittelen eller alternativtittelen matcher søkenavnet...
                if (normalizedMaster == sanName || normalizedAlt == sanName) {
                    // ...så øker vi poengsummen til MASTER-tittelen for denne raden!
                    matchCounts[master] = matchCounts.getOrDefault(master, 0) + 1
                }
            }
        }

        // Finn den mastertittelen som fikk flest poeng totalt på tvers av alle sine aliaser
        val bestMatch = matchCounts.maxByOrNull { it.value }?.key

        log.info("Batch search for $names gave best match '$bestMatch' with counts: $matchCounts")

        bestMatch
    }
}