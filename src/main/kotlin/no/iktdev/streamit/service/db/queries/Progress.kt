package no.iktdev.streamit.service.db.queries

import no.iktdev.streamit.service.Env
import no.iktdev.streamit.service.db.tables.content.ProgressTable
import no.iktdev.streamit.service.db.tables.content.SerieTable
import no.iktdev.streamit.service.db.tables.util.toEpochSeconds
import no.iktdev.streamit.service.dto.Episode
import no.iktdev.streamit.service.dto.Movie
import no.iktdev.streamit.service.dto.ProgressTableDto
import no.iktdev.streamit.service.dto.Serie
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.transactions.transaction

private fun ResultRow.isSerie(): Boolean {
    return this[ProgressTable.type].lowercase() == "serie"
}

private fun ResultRow.isMovie(): Boolean {
    return this[ProgressTable.type].lowercase() == "movie"
}


fun ProgressTable.executeGetAllOn(userId: String) =
    ProgressTable.selectRecords(userId)
        .map { ProgressTableDto.fromTable(it) }


fun ProgressTable.executeGetMoviesOn(userId: String) =
    ProgressTable.selectMovieRecords(userId)
        .map { ProgressTableDto.fromTable(it) }


fun ProgressTable.executeGetSeriesOn(userId: String) =
    ProgressTable.selectSerieRecords(userId)
        .map { ProgressTableDto.fromTable(it) }


fun ProgressTable.executeGetSeriesAfterOn(userId: String, time: Int) =
    ProgressTable.selectSerieRecordsAfter(userId, time)
        .map { ProgressTableDto.fromTable(it) }


fun ProgressTable.executeGetMoviesAfterOn(userId: String, time: Int) =
    ProgressTable.selectMovieRecordsAfter(userId, time)
        .map { ProgressTableDto.fromTable(it) }


fun ProgressTable.executeGetMovieWithTitleOn(userId: String, title: String) =
    ProgressTable.selectMovieRecordOnTitle(userId, title)
        .map { ProgressTableDto.fromTable(it) }
        .singleOrNull()


fun ProgressTable.executeGetSeriesWithCollectionOn(userId: String, collection: String) =
    ProgressTable.selectSerieRecordOnCollection(userId, collection)
        .map { ProgressTableDto.fromTable(it) }


fun ProgressTable.executeUpsertMovieOn(userId: String, movie: Movie) = transaction {
    try {
        ProgressTable.upsertMovieRecord(
            userId = userId,
            title = movie.title,
            collection = movie.collection,
            progress = movie.progress.toInt(),
            duration = movie.duration.toInt(),
            played = movie.played.toInt(),
            videoFile = movie.video
        )
    } catch (e: Exception) {
        e.printStackTrace()
        throw e
    }
}

fun ProgressTable.executeUpsertSerieOn(userId: String, serie: Serie) = transaction {
    val converted = ProgressTableDto.fromSerieToList(userId, serie)
    converted.forEach {
        ProgressTable.upsertSerieRecord(
            userId = it.guid,
            title = it.title,
            episode = it.episode!!,
            season = it.season!!,
            collection = it.collection,
            progress = it.progress,
            duration = it.duration,
            played = it.played,
            videoFile = it.video
        )
    }
}

fun ProgressTable.executeResumeOrNextEpisode(userId: String) = transaction {
    val resume = ProgressTable.selectResumeEpisode(userId, Env.continueWatch, 10)
        .groupBy { it -> it[SerieTable.collection] }
        .mapNotNull { it ->
            it.value.firstOrNull()?.let {
                Episode(
                    season =  it[SerieTable.season],
                    episode = it[SerieTable.episode],
                    title = it[SerieTable.title],
                    video = it[SerieTable.video],
                    progress = it[ProgressTable.progress],
                    duration = it[ProgressTable.duration],
                    played = it[ProgressTable.played]?.toEpochSeconds()?.toInt() ?: 0
                )
            }?.let { episode ->
                Serie.basedOn(it.value.first()).apply {
                    episodes = listOf(episode)
                }
            }
        }

    val completed = ProgressTable.selectCompletedEpisodes(userId, Env.continueWatch, 10)
        .groupBy { it -> it[SerieTable.collection] }
        .mapNotNull { it ->
            val serie = Serie.basedOn(it.value.first())
            val r = it.value.first()
            val nextEpisode = ProgressTable.selectNextEpisode(serie.collection, r[SerieTable.season], r[SerieTable.episode])
                .map { it ->
                    Episode(
                        season =  it[SerieTable.season],
                        episode = it[SerieTable.episode],
                        title = it[SerieTable.title],
                        video = it[SerieTable.video]
                    )
                }

            serie.apply {
                this.episodes = nextEpisode
            }
        }

    resume + completed
}