package no.iktdev.streamit.service.model.shared.consumer.content

import no.iktdev.streamit.service.model.shared.ContentType

data class ContinueToWatch(
    val catalogId: Long,
    val title: String,
    val cover: String?,
    val type: ContentType,
    val video: Video,
    val progress: Progress?,
    val episode: ContinueWatchingEpisode? = null
)

data class ContinueWatchingEpisode(
    val season: Int,
    val number: Int,
    val title: String?
)