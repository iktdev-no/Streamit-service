package no.iktdev.streamit.service.model.shared.content

data class Episode(
    val season: Int,
    val episode: Int,
    val title: String?,
    val video: Video,
    var subtitles: List<Subtitle> = emptyList(),
    val progress: Progress? = null,
)