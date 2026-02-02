package no.iktdev.streamit.service.services

import mu.KotlinLogging
import no.iktdev.streamit.service.db.tables.content.CatalogTable
import no.iktdev.streamit.service.db.tables.content.GenreTable
import no.iktdev.streamit.service.db.tables.content.MovieTable
import no.iktdev.streamit.service.db.tables.content.MovieTableObject
import no.iktdev.streamit.service.db.tables.content.SerieTable
import no.iktdev.streamit.service.db.tables.content.SubtitleTable
import no.iktdev.streamit.service.db.tables.content.SummaryTable
import no.iktdev.streamit.service.db.tables.content.TitleTable
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.dto.MediaProcesserImport
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.insertIgnoreAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ImportContentService {
    private val log = KotlinLogging.logger {}

    fun importContent(import: MediaProcesserImport): Boolean {

        // 1. Insert movie or serie → get iid (Int?) or fail
        val iid: Int? = when (import.metadata.mediaType) {
            MediaProcesserImport.MediaType.Movie -> insertMovieAndGetId(import)
            MediaProcesserImport.MediaType.Serie -> {
                val ok = insertSerie(import)
                if (!ok) return false
                null
            }
        }

        // 2. Insert subtitles
        import.media?.subtitles?.forEach { subtitle ->
            withTransaction {
                SubtitleTable.insertAndIgnore(
                    import.collection,
                    subtitle.language,
                    subtitle.subtitleFile,
                    import.media.videoFile ?: subtitle.subtitleFile
                )
            }.onFailure { log.error("Error while importing subtitle $subtitle", it) }
        }

        // 3. Resolve genres
        val genreIds = resolveGenres(import)

        // 4. Find or create catalog
        val catalogId = findCatalogId(import)
            ?: insertCatalog(import, genreIds, iid)

        // 5. Update cover if missing
        updateCoverIfMissing(catalogId, import)

        // 6. Insert summaries
        import.metadata.summary.forEach { summary ->
            withTransaction {
                SummaryTable.insertIgnore(catalogId, summary.language, summary.description)
            }
        }

        // 7. Insert titles (master + alternatives)
        upsertTitles(import)

        return true
    }

    fun importMetadata(import: MediaProcesserImport): Boolean {

        // 1. Resolve genres
        val genreIds = resolveGenres(import)

        // 2. Find catalog (must exist — metadata skal ikke opprette nye kataloger)
        val catalogId = findCatalogId(import)
            ?: return false // metadata kan ikke opprette nye kataloger

        // 3. Update cover if missing
        updateCoverIfMissing(catalogId, import)

        // 4. Insert summaries
        import.metadata.summary.forEach { summary ->
            withTransaction {
                SummaryTable.insertIgnore(catalogId, summary.language, summary.description)
            }
        }

        // 5. Insert titles (master + alternatives)
        upsertTitles(import)

        return true
    }



    private fun findCatalogId(import: MediaProcesserImport): Int? {
        val type = import.metadata.mediaType.name.lowercase()
        val collection = import.collection

        return withTransaction {
            CatalogTable
                .select(CatalogTable.id)
                .where {
                    (CatalogTable.collection eq collection) and
                            (CatalogTable.type eq type)
                }
                .singleOrNull()
                ?.get(CatalogTable.id)
                ?.value
        }.getOrNull()
    }

    private fun insertCatalog(
        import: MediaProcesserImport,
        genreIds: List<Int>,
        iid: Int?
    ): Int {

        val type = import.metadata.mediaType.name.lowercase()
        val collection = import.collection
        val title = import.metadata.title

        return withTransaction {
            CatalogTable.insertAndGetId {
                it[CatalogTable.title] = title
                it[CatalogTable.collection] = collection
                it[CatalogTable.cover] = import.metadata.cover
                it[CatalogTable.type] = type
                it[CatalogTable.genres] = genreIds.joinToString(",")
                it[CatalogTable.iid] = iid
                it[CatalogTable.added] = LocalDateTime.now()
            }.value
        }.getOrThrow()
    }

    private fun updateCoverIfMissing(catalogId: Int, import: MediaProcesserImport) {
        val cover = import.metadata.cover ?: return

        withTransaction {
            CatalogTable.update(
                where = { (CatalogTable.id eq catalogId) and CatalogTable.cover.isNull() }
            ) {
                it[CatalogTable.cover] = cover
            }
        }
    }

    private fun insertMovieAndGetId(import: MediaProcesserImport): Int {
        if (import.media?.videoFile == null) {
            throw IllegalArgumentException("Missing video file!")
        }
        val exists = withTransaction {
            MovieTable.selectAll()
                .where { MovieTable.video eq import.media.videoFile }
                .singleOrNull()?.let { MovieTableObject.fromRow(it) }
        }.getOrNull()?.id
        if (exists != null) {
            return exists
        }
        return withTransaction {
            MovieTable.insertMovie(import.media.videoFile)
        }.getOrThrow()!!.value
    }

    private fun insertSerie(import: MediaProcesserImport): Boolean {
        val episodeInfo = import.episodeInfo
            ?: throw IllegalStateException("Episode info not available, required for series type")
        val video = import.media!!.videoFile
            ?: throw IllegalStateException("Video file not available, required for series type")

        val result = withTransaction {
            SerieTable.insertSerie(
                title = episodeInfo.episodeTitle,
                collection = import.collection,
                episode = episodeInfo.episodeNumber,
                season = episodeInfo.seasonNumber,
                videoFile = video
            )
        }
        return result.isSuccess
    }


    private fun resolveGenres(import: MediaProcesserImport): List<Int> {
        val genres = import.metadata.genres
        if (genres.isEmpty()) return emptyList()

        return withTransaction {
            genres.mapNotNull { genre ->
                GenreTable.insertIgnoreAndGetId {
                    it[GenreTable.genre] = genre
                }?.value
            }
        }.getOrElse { emptyList() }
    }

    private fun upsertTitles(import: MediaProcesserImport) {
        val master = import.metadata.title
        val alternatives = import.metadata.alternativeTitles ?: emptyList()

        if (alternatives.isEmpty()) return

        withTransaction {
            // 1. Finn eksisterende masterTitle hvis noen av titlene finnes
            val allTitles = listOf(master) + alternatives

            val existing = TitleTable
                .select(TitleTable.masterTitle, TitleTable.alternativeTitle)
                .where {
                    (TitleTable.masterTitle inList allTitles) or
                            (TitleTable.alternativeTitle inList allTitles)
                }
                .firstOrNull()

            val resolvedMaster: String = if (existing != null) {
                existing[TitleTable.masterTitle]
            } else {
                master
            }

            // 2. Sett inn alle alternative titler som ikke finnes fra før
            alternatives.forEach { alt ->
                TitleTable.insertIgnoreAndGetId {
                    it[TitleTable.masterTitle] = resolvedMaster
                    it[TitleTable.alternativeTitle] = alt
                }
            }

            // 3. Hvis ingen eksisterende rad ble funnet, må vi også lagre masterTitle selv
            if (existing == null) {
                TitleTable.insertIgnoreAndGetId {
                    it[TitleTable.masterTitle] = resolvedMaster
                    it[TitleTable.alternativeTitle] = master
                }
            }
        }
    }


}