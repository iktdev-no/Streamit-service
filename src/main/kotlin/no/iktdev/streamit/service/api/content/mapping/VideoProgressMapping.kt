package no.iktdev.streamit.service.api.content.mapping

import no.iktdev.streamit.library.db.tables.content.ProgressTable
import no.iktdev.streamit.shared.classes.BaseProgress
import no.iktdev.streamit.shared.classes.ProgressEpisode
import no.iktdev.streamit.shared.classes.ProgressMovie
import no.iktdev.streamit.shared.classes.ProgressSerie
import no.iktdev.streamit.shared.classes.ProgressTableDto
import no.iktdev.streamit.shared.classes.Serie
import no.iktdev.streamit.shared.classes.isMovie
import no.iktdev.streamit.shared.classes.isSerie

fun List<ProgressTableDto>.toMixedList(): List<BaseProgress> {
    return this.filter { it.isMovie() }
        .map { ProgressMovie.fromProgressTable(it) } +
            toSerie()
}

fun List<ProgressTableDto>.toSerie(): List<ProgressSerie> {
    return this.filter { it.isSerie() }
        .groupBy { it.collection }
        .filter { it -> it.value.isEmpty() }
        .map { (_, entries) ->
            ProgressSerie.fromProgressTable(entries.first()).apply {
                this.episodes = entries.mapNotNull { ep -> ProgressEpisode.fromFlat(ep) }
            }
        }
}