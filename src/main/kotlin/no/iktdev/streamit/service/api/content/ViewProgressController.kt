package no.iktdev.streamit.service.api.content

import com.google.gson.Gson
import no.iktdev.streamit.service.db.tables.util.executeWithResult
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.api.content.mapping.toMixedList
import no.iktdev.streamit.service.api.content.mapping.toSerie
import no.iktdev.streamit.service.db.queries.executeFindForVideo
import no.iktdev.streamit.service.db.queries.executeGetAllOn
import no.iktdev.streamit.service.db.queries.executeGetMovieWithTitleOn
import no.iktdev.streamit.service.db.queries.executeGetMoviesAfterOn
import no.iktdev.streamit.service.db.queries.executeGetMoviesOn
import no.iktdev.streamit.service.db.queries.executeGetSeriesAfterOn
import no.iktdev.streamit.service.db.queries.executeGetSeriesOn
import no.iktdev.streamit.service.db.queries.executeGetSeriesWithCollectionOn
import no.iktdev.streamit.service.db.queries.executeResumeOrNextEpisode
import no.iktdev.streamit.service.db.tables.content.ProgressTable
import no.iktdev.streamit.service.db.tables.content.SubtitleTable
import no.iktdev.streamit.service.log
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.db.tables.util.UpsertResult
import no.iktdev.streamit.service.dto.BaseProgress
import no.iktdev.streamit.service.dto.Movie
import no.iktdev.streamit.service.dto.ProgressMovie
import no.iktdev.streamit.service.dto.ProgressSerie
import no.iktdev.streamit.service.dto.Response
import no.iktdev.streamit.service.dto.Serie
import no.iktdev.streamit.service.isDebug
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@ApiRestController
@RequestMapping("/progress")
class ViewProgressController {

    @RequiresAuthentication(Scope.ProgressRead)
    @GetMapping("/{guid}")
    fun allProgressOnGuid(@PathVariable guid: String): List<BaseProgress> {
        return ProgressTable.executeGetAllOn(guid).toMixedList()
    }

    @RequiresAuthentication(Scope.ProgressRead)
    @GetMapping(value = [
        "/{guid}/movie",
        "/{guid}/movie/after/{time}"
    ])
    fun allProgressOnGuidForMovies(@PathVariable guid: String, @PathVariable time: Int? = null): List<ProgressMovie> {
        return (if (time == null)
            ProgressTable.executeGetMoviesOn(guid) else
            ProgressTable.executeGetMoviesAfterOn(guid, time))
            .map { ProgressMovie.fromProgressTable(it) }
    }

    @RequiresAuthentication(Scope.ProgressRead)
    @GetMapping(value = [
        "/{guid}/serie",
        "/{guid}/serie/after/{time}"
    ])
    fun allProgressOnGuidForSerie(@PathVariable guid: String, @PathVariable time: Int? = null): List<ProgressSerie> {
        val result = if (time == null)
            ProgressTable.executeGetSeriesOn(guid) else
            ProgressTable.executeGetSeriesAfterOn(guid, time)
        return result.toSerie()
    }

    @RequiresAuthentication(Scope.ProgressRead)
    @GetMapping("/{guid}/movie/{title}")
    fun getProgressForUserWithMovieTitle(@PathVariable guid: String, @PathVariable title: String): ProgressMovie? {
        return ProgressTable.executeGetMovieWithTitleOn(guid, title)?.let { it ->
            ProgressMovie.fromProgressTable(it)
        }
    }

    @RequiresAuthentication(Scope.ProgressRead)
    @GetMapping("/{guid}/serie/{collection}")
    fun getProgressForUserWithSerieCollection(@PathVariable guid: String, @PathVariable collection: String): ProgressSerie? {
        return ProgressTable.executeGetSeriesWithCollectionOn(guid, collection)
            .toSerie()
            .singleOrNull()

    }

    @RequiresAuthentication(Scope.ProgressRead)
    @GetMapping("/{guid}/continue/serie")
    fun getContinueSerie(@PathVariable guid: String): List<Serie> {
        return ProgressTable.executeResumeOrNextEpisode(guid).map { serie ->
            serie.apply {
                serie.episodes.map { episode ->
                    episode.apply {
                        subtitles = SubtitleTable.executeFindForVideo(episode.video)
                    }
                }
            }
        }
    }

    /**
     * Post mapping below
     **/

    @RequiresAuthentication(Scope.ProgressWrite)
    @PostMapping("/{guid}/movie")
    @ResponseStatus(HttpStatus.OK)
    fun uploadedProgressMovieOnGuid(@PathVariable guid: String, @RequestBody progress: Movie) : ResponseEntity<String> {
        val result: UpsertResult? = try {
            withTransaction {
                ProgressTable.upsertMovieRecord(
                    userId = guid,
                    title = progress.title,
                    collection = progress.collection,
                    progress = progress.progress,
                    duration = progress.duration,
                    played = progress.played,
                    videoFile = progress.video
                )
            }.getOrNull()
        } catch (e: Exception) {
            log.error { "An error occurred while updating movie progress for user $guid with title ${progress.title}." }
            e.printStackTrace()
            null
        }
        return when (result) {
            is UpsertResult.Inserted, UpsertResult.Updated -> ResponseEntity.ok(Gson().toJson(Response()))
            UpsertResult.Skipped -> {
                log.warn { "Skipping update of movie progress for user $guid with title ${progress.title} as it already exists." }
                ResponseEntity.unprocessableEntity().body(Gson().toJson(Response(message = "Update of movie progress was skipped..")))
            }
            else -> {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Gson().toJson(Response(message = "An error occurred while updating movie progress.")))
            }
        }
    }

    @RequiresAuthentication(Scope.ProgressWrite)
    @PostMapping("/{guid}/serie")
    @ResponseStatus(HttpStatus.OK)
    fun uploadedProgressSerieOnGuid(@PathVariable guid: String, @RequestBody progress: Serie): ResponseEntity<String> {
        if (isDebug()) {
            log.info { "Performing serie progress update on ${progress.collection}\n ${Gson().toJson(progress)}" }
        }
        val result = executeWithResult {
            progress.episodes.map {
                ProgressTable.upsertSerieRecord(
                    userId = guid,
                    title = progress.title,
                    episode = it.episode,
                    season = it.season,
                    collection = progress.collection,
                    progress = it.progress,
                    duration = it.duration,
                    played = it.played,
                    videoFile = it.video
                )
            }
        }
        if (result?.any { it == UpsertResult.Skipped } == true) {
            log.warn { "Skipping update of serie progress for user $guid with collection ${progress.collection} as it already exists." }
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(Gson().toJson(Response(message = "One or more episode was unsuccessful..")))
        } else if (result == null) {
            log.error { "An error occurred while updating serie progress for user $guid with collection ${progress.collection}." }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Gson().toJson(Response(message = "An error occurred while updating serie progress.")))
        }
        return ResponseEntity.ok(Gson().toJson(Response()))
    }

}