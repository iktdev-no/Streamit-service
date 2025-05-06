package no.iktdev.streamit.shared.database.queries

import no.iktdev.streamit.shared.classes.Movie
import no.iktdev.streamit.shared.classes.Serie
import no.iktdev.streamit.library.db.tables.content.ProgressTable
import no.iktdev.streamit.library.db.tables.content.SerieTable
import no.iktdev.streamit.shared.Env
import no.iktdev.streamit.shared.classes.Episode
import no.iktdev.streamit.shared.classes.ProgressTableDto
import no.iktdev.streamit.shared.database.toEpochSeconds
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.transactions.transaction

private fun ResultRow.isSerie(): Boolean {
    return this[ProgressTable.type].lowercase() == "serie"
}

private fun ResultRow.isMovie(): Boolean {
    return this[ProgressTable.type].lowercase() == "movie"
}


fun ProgressTable.executeGetAllOn(userId: String) = transaction {
    ProgressTable.selectRecords(userId)
        .mapNotNull { ProgressTableDto.fromRow(it) }
}

fun ProgressTable.executeGetMoviesOn(userId: String) = transaction {
    ProgressTable.selectMovieRecords(userId)
        .mapNotNull { ProgressTableDto.fromRow(it) }
}

fun ProgressTable.executeGetSeriesOn(userId: String) = transaction {
    ProgressTable.selectSerieRecords(userId)
        .mapNotNull { ProgressTableDto.fromRow(it) }
}

fun ProgressTable.executeGetSeriesAfterOn(userId: String, time: Int) = transaction {
    ProgressTable.selectSerieRecordsAfter(userId, time)
        .mapNotNull { ProgressTableDto.fromRow(it) }
}

fun ProgressTable.executeGetMoviesAfterOn(userId: String, time: Int) = transaction {
    ProgressTable.selectMovieRecordsAfter(userId, time)
        .mapNotNull { ProgressTableDto.fromRow(it) }
}

fun ProgressTable.executeGetMovieWithTitleOn(userId: String, title: String) = transaction {
    ProgressTable.selectMovieRecordOnTitle(userId, title)
        .mapNotNull { ProgressTableDto.fromRow(it) }
        .singleOrNull()
}

fun ProgressTable.executeGetSeriesWithCollectionOn(userId: String, collection: String) = transaction {
    ProgressTable.selectSerieRecordOnCollection(userId, collection)
        .mapNotNull { ProgressTableDto.fromRow(it) }
}

fun ProgressTable.executeGetLastEpisodeOn(userId: String) = transaction {
    ProgressTable.selectLastEpisode(userId, Env.continueWatch)
        .mapNotNull { ProgressTableDto.fromRow(it) }
}

fun ProgressTable.executeUpsertMovieOn(userId: String, movie: Movie) = transaction {
    ProgressTable.upsertMovieRecord(
        userId = userId,
        title = movie.title,
        collection = movie.collection,
        progress = movie.progress,
        duration = movie.duration,
        played = movie.played,
        videoFile = movie.video
    )
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

fun ProgressTable.executeResumeOrNext(userId: String) = transaction {
    ProgressTable.selectResumeOrNext(userId, Env.continueWatch)
        .groupBy { it[SerieTable.collection] }
        .mapNotNull { it ->
            Serie.basedOn(it.value.first()).apply {
                episodes = it.value.map {
                    Episode(
                        season =  it[SerieTable.season],
                        episode = it[SerieTable.episode],
                        title = it[SerieTable.title],
                        video = it[SerieTable.video],
                        progress = it[ProgressTable.progress],
                        duration = it[ProgressTable.duration],
                        played = it[ProgressTable.played]?.toEpochSeconds()?.toInt() ?: 0
                    )
                }
            }
        }
}