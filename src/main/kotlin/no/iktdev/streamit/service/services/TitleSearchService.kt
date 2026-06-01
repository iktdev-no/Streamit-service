package no.iktdev.streamit.service.services

import no.iktdev.streamit.service.db.tables.content.TitleTable
import no.iktdev.streamit.service.db.tables.util.withTransaction
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Service

@Service
class TitleSearchService {

    fun sanitizeTitle(input: String): String {
        return input
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