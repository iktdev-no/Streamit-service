package no.iktdev.streamit.service.api.content

import com.google.gson.Gson
import no.iktdev.streamit.library.db.ext.UpsertResult
import no.iktdev.streamit.library.db.tables.content.ProgressTable
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.api.content.mapping.toMixedList
import no.iktdev.streamit.service.api.content.mapping.toSerie
import no.iktdev.streamit.shared.Mode
import no.iktdev.streamit.shared.RequiresAuthentication
import no.iktdev.streamit.shared.classes.BaseProgress
import no.iktdev.streamit.shared.classes.Movie
import no.iktdev.streamit.shared.classes.ProgressMovie
import no.iktdev.streamit.shared.classes.ProgressSerie
import no.iktdev.streamit.shared.classes.Response
import no.iktdev.streamit.shared.classes.Serie
import no.iktdev.streamit.shared.database.queries.executeGetAllOn
import no.iktdev.streamit.shared.database.queries.executeGetMovieWithTitleOn
import no.iktdev.streamit.shared.database.queries.executeGetMoviesAfterOn
import no.iktdev.streamit.shared.database.queries.executeGetMoviesOn
import no.iktdev.streamit.shared.database.queries.executeGetSeriesAfterOn
import no.iktdev.streamit.shared.database.queries.executeGetSeriesOn
import no.iktdev.streamit.shared.database.queries.executeGetSeriesWithCollectionOn
import no.iktdev.streamit.shared.database.queries.executeResumeOrNext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus

@ApiRestController
@RequestMapping("/api/progress")
class ViewProgressController {

    @RequiresAuthentication(Mode.Soft)
    @GetMapping("/{guid}")
    fun allProgressOnGuid(@PathVariable guid: String): List<BaseProgress> {
        return ProgressTable.executeGetAllOn(guid).toMixedList()
    }

    @RequiresAuthentication(Mode.Soft)
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

    @RequiresAuthentication(Mode.Soft)
    @GetMapping(value = [
        "/{guid}/serie",
        "/{guid}/serie/after/{time}"
    ])
    fun allProgressOnGuidForSerie(@PathVariable guid: String, @PathVariable time: Int? = null): List<ProgressSerie> {
        return (if (time == null)
            ProgressTable.executeGetSeriesOn(guid) else
            ProgressTable.executeGetSeriesAfterOn(guid, time))
            .toSerie()
    }

    @RequiresAuthentication(Mode.Soft)
    @GetMapping("/{guid}/movie/{title}")
    fun getProgressForUserWithMovieTitle(@PathVariable guid: String, @PathVariable title: String): ProgressMovie? {
        return ProgressTable.executeGetMovieWithTitleOn(guid, title)?.let { it ->
            ProgressMovie.fromProgressTable(it)
        }
    }

    @RequiresAuthentication(Mode.Soft)
    @GetMapping("/{guid}/serie/{collection}")
    fun getProgressForUserWithSerieCollection(@PathVariable guid: String, @PathVariable collection: String): ProgressSerie? {
        return ProgressTable.executeGetSeriesWithCollectionOn(guid, collection)
            .toSerie()
            .singleOrNull()

    }

    @RequiresAuthentication(Mode.Soft)
    @GetMapping("/{guid}/continue/serie")
    fun getContinueSerie(@PathVariable guid: String): List<Serie> {
        return ProgressTable.executeResumeOrNext(guid)
    }

    /**
     * Post mapping below
     **/

    @RequiresAuthentication(Mode.Strict)
    @PostMapping("/{guid}/movie")
    @ResponseStatus(HttpStatus.OK)
    fun uploadedProgressMovieOnGuid(@PathVariable guid: String, @RequestBody progress: Movie) : ResponseEntity<String> {
        val result = ProgressTable.upsertMovieRecord(
            userId = guid,
            title = progress.title,
            collection = progress.collection,
            progress = progress.progress,
            duration = progress.duration,
            played = progress.played,
            videoFile = progress.video
        )
        return when (result) {
            is UpsertResult.Inserted, UpsertResult.Updated -> ResponseEntity.ok(Gson().toJson(Response()))
            UpsertResult.Skipped -> ResponseEntity.unprocessableEntity().body(Gson().toJson(Response(message = "Update of movie progress was skipped..")))
        }
    }

    @RequiresAuthentication(Mode.Strict)
    @PostMapping("/{guid}/serie")
    @ResponseStatus(HttpStatus.OK)
    fun uploadedProgressSerieOnGuid(@PathVariable guid: String, @RequestBody progress: Serie): ResponseEntity<String> {
        val result = progress.episodes.map {
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
        if (result.any { it == UpsertResult.Skipped }) {
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(Gson().toJson(Response(message = "One or more episode was unsuccessful..")))
        }
        return ResponseEntity.ok(Gson().toJson(Response()))
    }

}