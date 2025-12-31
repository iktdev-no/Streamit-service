package no.iktdev.streamit.service.api.import

import mu.KotlinLogging
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.db.tables.content.CatalogTable
import no.iktdev.streamit.service.db.tables.content.GenreTable
import no.iktdev.streamit.service.db.tables.content.MovieTable
import no.iktdev.streamit.service.db.tables.content.SerieTable
import no.iktdev.streamit.service.db.tables.content.SubtitleTable
import no.iktdev.streamit.service.db.tables.content.SummaryTable
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.dto.MediaProcesserImport
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnoreAndGetId
import org.jetbrains.exposed.sql.update
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import java.time.LocalDateTime

@ApiRestController
@RequestMapping("/mediaprocesser")
class MediaProcesserImportController {

    val log = KotlinLogging.logger {}

    @RequiresAuthentication(Scope.MediaWrite)
    @PostMapping("/import")
    fun import(@RequestBody import: MediaProcesserImport): ResponseEntity<Void> {

        val videoFile = import.media?.videoFile
        val iid = if (videoFile != null) {
            if (import.metadata.mediaType == MediaProcesserImport.MediaType.Movie) {
                val result = withTransaction {
                    MovieTable.insertMovie(videoFile)?.value
                }
                if (result.isFailure) {
                    return ResponseEntity(HttpStatus.BAD_REQUEST)
                }
                result.getOrNull()
            } else {
                val episodeInfo = import.episodeInfo
                    ?: throw IllegalStateException("Episode info not available, which is required when providing serie type")
                val serieInsertResult = withTransaction {
                    SerieTable.insertSerie(
                        title = episodeInfo.episodeTitle,
                        collection = import.collection,
                        episode = episodeInfo.episodeNumber,
                        season = episodeInfo.seasonNumber,
                        videoFile = videoFile,
                    )
                }
                if (serieInsertResult.isFailure) {
                    return ResponseEntity(HttpStatus.BAD_REQUEST)
                }

                null //Alltid null
            }
        } else null

        import.media?.subtitles?.forEach { subtitle ->
            withTransaction {
                SubtitleTable.insertAndIgnore(
                    import.collection,
                    subtitle.language,
                    subtitle.subtitleFile,
                    videoFile ?: subtitle.subtitleFile
                )
            }.getOrElse { log.error("Error while importing $subtitle file", it) }
        }

        val genreIds = import.metadata.genres.takeIf { it.isNotEmpty() }?.let { genres ->
            withTransaction {
                genres.mapNotNull { genre ->
                    GenreTable.insertIgnoreAndGetId {
                        it[GenreTable.genre] = genre
                    }?.value
                }
            }.getOrNull()
        } ?: emptyList()


        val catalogId = withTransaction {
            val insertedId = CatalogTable.insertIgnoreAndGetId {
                it[title] = import.metadata.title
                it[collection] = import.collection
                it[cover] = import.metadata.cover
                it[type] = import.metadata.mediaType.name.lowercase()
                it[genres] = genreIds.joinToString(",")
                it[CatalogTable.iid] = iid
                it[added] = LocalDateTime.now()
            }?.value

            insertedId ?: CatalogTable.select(CatalogTable.id)
                .where {
                    (CatalogTable.title eq import.metadata.title) and
                            (CatalogTable.collection eq import.collection)
                }.single()[CatalogTable.id].value
        }.getOrThrow()



        if (import.metadata.cover != null) {
            withTransaction {
                CatalogTable.update(where = { (CatalogTable.id eq catalogId) and CatalogTable.cover.isNull() }) {
                    it[CatalogTable.cover] = import.metadata.cover
                }
            }
        }

        import.metadata.summary.forEach { summary ->
            withTransaction {
                SummaryTable.insertIgnore(catalogId, summary.language, summary.description)
            }
        }

        return ResponseEntity(HttpStatus.OK)
    }

}