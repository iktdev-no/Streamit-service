package no.iktdev.streamit.service.services

import no.iktdev.streamit.service.model.shared.contentImport.CatalogMetadata
import no.iktdev.streamit.service.model.shared.contentImport.Media
import no.iktdev.streamit.service.model.shared.contentImport.MediaContent
import no.iktdev.streamit.service.model.shared.contentImport.MediaImportV2
import no.iktdev.streamit.service.stores.catalog.ICatalogStore
import no.iktdev.streamit.service.stores.genre.IGenreStore
import no.iktdev.streamit.service.stores.movie.IMovieStore
import no.iktdev.streamit.service.stores.serie.ISerieStore
import no.iktdev.streamit.service.stores.subtitle.ISubtitleStore
import no.iktdev.streamit.service.stores.summary.ISummaryStore
import no.iktdev.streamit.service.stores.title.ITitleStore
import no.iktdev.streamit.service.stores.video.IVideoStore
import org.springframework.stereotype.Service

@Service
class ImportContentService(
    private val catalogStore: ICatalogStore,
    private val titleStore: ITitleStore,
    private val genreStore: IGenreStore,
    private val summaryStore: ISummaryStore,
    private val videoStore: IVideoStore,
    private val movieStore: IMovieStore,
    private val serieStore: ISerieStore,
    private val subtitleStore: ISubtitleStore,
) {

    fun import(input: MediaImportV2): Boolean {
        input.validate()

        val catalogId = resolveCatalog(input)

        input.metadata?.let {
            importMetadata(catalogId, it)
        }

        input.media?.let {
            importMedia(catalogId, it)
        }

        return true
    }

    private fun resolveCatalog(input: MediaImportV2): Long {
        input.reference?.catalogId?.let {
            return it
        }

        val metadata = requireNotNull(input.metadata) {
            "Metadata is required when creating a new catalog"
        }

        return catalogStore.insert(
            store = requireNotNull(input.reference).store,
            type = metadata.type,
            cover = metadata.cover
        )
    }

    private fun importMetadata(
        catalogId: Long,
        metadata: CatalogMetadata
    ) {
        metadata.title?.let { title ->
            titleStore.insert(
                catalogId = catalogId,
                title = title,
                language = "eng",
                preferred = true
            )
        }

        metadata.alternativeTitles.forEach { title ->
            titleStore.insert(
                catalogId = catalogId,
                title = title,
                language = "eng",
                preferred = false
            )
        }

        metadata.genres.forEach { genreName ->
            genreStore.getOrInsert(genreName)
        }

        metadata.summaries.forEach { summary ->
            summaryStore.insert(
                catalogId = catalogId,
                description = summary.description,
                language = summary.language
            )
        }

        metadata.cover?.let { cover ->
            catalogStore.updateCover(catalogId, cover)
        }
    }

    private fun importMedia(
        catalogId: Long,
        media: Media
    ) {
        val content = media.content

        if (content != null) {
            val video = videoStore.getByFile(content.videoFile)
                ?: videoStore.insert(content.videoFile)

            when (content) {
                is MediaContent.Movie -> {
                    movieStore.insert(
                        catalogId = catalogId,
                        videoId = video.id
                    )
                }

                is MediaContent.Episode -> {
                    serieStore.insert(
                        catalogId = catalogId,
                        videoId = video.id,
                        season = content.season,
                        episode = content.episode,
                        title = content.title
                    )
                }
            }

            media.subtitles.forEach { subtitle ->
                subtitleStore.insert(
                    videoId = video.id,
                    language = subtitle.language,
                    format = subtitle.format,
                    file = subtitle.subtitleFile
                )
            }
        }
    }
}