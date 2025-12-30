package no.iktdev.streamit.service.api.content.mapping

import no.iktdev.streamit.service.dto.BaseProgress
import no.iktdev.streamit.service.dto.ProgressEpisode
import no.iktdev.streamit.service.dto.ProgressMovie
import no.iktdev.streamit.service.dto.ProgressSerie
import no.iktdev.streamit.service.dto.ProgressTableDto
import no.iktdev.streamit.service.dto.Serie
import no.iktdev.streamit.service.dto.isMovie
import no.iktdev.streamit.service.dto.isSerie

fun List<ProgressTableDto>.toMixedList(): List<BaseProgress> {
    return this.filter { it.isMovie() }
        .map { ProgressMovie.fromProgressTable(it) } +
            toSerie()
}

fun List<ProgressTableDto>.toSerie(): List<ProgressSerie> {
    return this.filter { it.isSerie() }
        .groupBy { it.collection }
        .filter { it -> it.value.isNotEmpty() }
        .map { (_, entries) ->
            ProgressSerie.fromProgressTable(entries.first()).apply {
                this.episodes = entries.mapNotNull { episodes -> ProgressEpisode.fromFlat(episodes) }
            }
        }
}