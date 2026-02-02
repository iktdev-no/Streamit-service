package no.iktdev.streamit.service.dto

data class MediaProcesserImport(
    val collection: String,
    val episodeInfo: EpisodeInfo? = null,
    val media: MediaImport? = null,
    val metadata: MetadataImport
) {

    data class MetadataImport(
        // Vi tar ikke med collection fra metadata, da det bestemmes i Migrate
        val title: String,
        val alternativeTitles: List<String> = emptyList(),
        val genres: List<String> = emptyList(),
        val cover: String? = null,
        val summary: List<Summary> = emptyList(),
        val mediaType: MediaType,
        val source: String? = null
    ) {
        data class Summary(
            val language: String, val description: String
        )
    }

    data class MediaImport(
        val videoFile: String?,
        val subtitles: List<Subtitle>,
    ) {
        data class Subtitle(
            val subtitleFile: String,
            val language: String
        )
    }

    data class EpisodeInfo(
        val episodeNumber: Int,
        val seasonNumber: Int,
        val episodeTitle: String? = null,
    )

    enum class MediaType {
        Movie,
        Serie
    }
}