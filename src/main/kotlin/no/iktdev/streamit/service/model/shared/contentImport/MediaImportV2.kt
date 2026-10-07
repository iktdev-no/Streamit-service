package no.iktdev.streamit.service.model.shared.contentImport

import no.iktdev.streamit.service.model.shared.CollectionReference
import no.iktdev.streamit.service.model.shared.ImportReference
import no.iktdev.streamit.service.model.shared.content.ContentType

data class MediaImportV2(
    val reference: ImportReference? = null,
    val metadata: CatalogMetadata? = null,
    val media: Media? = null,
) {
    fun validate() {
        require(metadata != null || media != null) {
            "Media import must contain metadata or media"
        }

        media?.let {
            require(it.content != null || it.subtitles.isNotEmpty()) {
                "Media must contain content or subtitles"
            }
        }
    }
}

data class CatalogMetadata(
    val title: String? = null,
    val alternativeTitles: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val cover: String? = null,
    val type: ContentType,
    val summaries: List<Summary> = emptyList()
)

data class Media(
    val content: MediaContent? = null,
    val subtitles: List<SubtitleImport> = emptyList()
)

sealed interface MediaContent {
    val videoFile: String

    data class Movie(
        override val videoFile: String
    ) : MediaContent

    data class Episode(
        override val videoFile: String,
        val season: Int,
        val episode: Int,
        val title: String? = null
    ) : MediaContent
}

data class SubtitleImport(
    val subtitleFile: String,
    val language: String,
    val format: String,
)

data class Summary(
    val language: String, val description: String
)