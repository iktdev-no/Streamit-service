package no.iktdev.streamit.shared.database.queries

import no.iktdev.streamit.shared.classes.Episode
import no.iktdev.streamit.shared.classes.Serie
import no.iktdev.streamit.library.db.tables.content.SerieTable
import no.iktdev.streamit.library.db.tables.content.SubtitleTable
import no.iktdev.streamit.shared.classes.Subtitle
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

private fun List<ResultRow>.toSerieObject(): Serie? {
    return this.firstOrNull()?.let {
        Serie.basedOn(it)
    }
}

private fun List<ResultRow>.toEpisodeList(): List<Episode> {
    return this.map {
        Episode(
            season = it[SerieTable.season],
            episode = it[SerieTable.episode],
            title = it[SerieTable.title],
            video = it[SerieTable.video]
        )
    }
}

fun Map<String, List<Subtitle>>.applyOnEpisodes(episodes: List<Episode>) {
    this.forEach { subtitle ->
        episodes.firstOrNull { e -> File(e.video).nameWithoutExtension == subtitle.key }?.let {
            it.subtitles = subtitle.value
        }
    }
}

fun SerieTable.executeSelectOnId(id: Int): Serie? = transaction {
    val table = SerieTable.selectOnId(id).filterNotNull()
    val episodes = table.map {
        Episode(
            season = it[SerieTable.season],
            episode = it[SerieTable.episode],
            title = it[SerieTable.title],
            video = it[SerieTable.video]
        )
    }

    val serie = table.toSerieObject() ?: return@transaction null

    SubtitleTable.findSubtitleForCollection(serie.collection)
        .map { Subtitle.fromRow(it) }.groupBy { it.associatedWithVideo }
        .applyOnEpisodes(episodes)

    serie.apply {
        this.episodes = episodes
    }
}

fun SerieTable.executeSelectOn(collection: String): Serie? = transaction {
    val table = SerieTable.selectOnCollection(collection).filterNotNull()
    val episodes = table.map {
        Episode(
            season = it[SerieTable.season],
            episode = it[SerieTable.episode],
            title = it[SerieTable.title],
            video = it[SerieTable.video]
        )
    }

    val serie = table.toSerieObject() ?: return@transaction null

    SubtitleTable.findSubtitleForCollection(serie.collection)
        .map { Subtitle.fromRow(it) }.groupBy { it.associatedWithVideo }
        .applyOnEpisodes(episodes)

    serie.apply {
        this.episodes = episodes
    }
}