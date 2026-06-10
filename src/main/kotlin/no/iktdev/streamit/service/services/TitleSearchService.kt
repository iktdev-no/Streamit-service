package no.iktdev.streamit.service.services

import no.iktdev.streamit.service.db.tables.content.TitleTable
import no.iktdev.streamit.service.db.tables.util.withTransaction
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Service
import java.text.Normalizer

@Service
class TitleSearchService {

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

    fun sanitizeTitle(input: String): String {
        return input.normalize()
            .lowercase()
            .replace("’", "'")                 // fancy apostrof → normal
            .replace("[^a-z0-9']".toRegex(), " ") // fjern alt unntatt bokstaver, tall og '
            .replace("'s\\b".toRegex(), "")    // fjern possessive 's
            .replace("'\\b".toRegex(), "")     // fjern apostrof på slutten av ord
            .replace("\\s+".toRegex(), " ")    // normaliser whitespace
            .trim()
    }

    fun findMasterBySanitized(raw: String, database: Database? = null, onError: ((Exception) -> Unit)? = null): String? = withTransaction(database, onError) {
        val sanitized = sanitizeTitle(raw)

        if (sanitized.isBlank()) {
            return@withTransaction null
        }

        TitleTable
            .selectAll()
            .firstOrNull { row ->
                val masterSan = sanitizeTitle(row[TitleTable.masterTitle])
                val altSan = sanitizeTitle(row[TitleTable.alternativeTitle])

                masterSan == sanitized || altSan == sanitized
            }
            ?.get(TitleTable.masterTitle)
    }




}