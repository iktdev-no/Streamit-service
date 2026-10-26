package no.iktdev.streamit.service.services

import mu.KotlinLogging
import no.iktdev.files.IFile
import no.iktdev.streamit.service.Env
import no.iktdev.streamit.service.model.shared.importing.CatalogMetadata
import no.iktdev.streamit.service.model.shared.importing.Media
import no.iktdev.streamit.service.model.shared.importing.MediaType
import no.iktdev.streamit.service.model.shared.importing.MediaImportV2
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
    private val log = KotlinLogging.logger {}

    fun import(input: MediaImportV2): Boolean {
        input.validate()
        if (!validateFiles(input)) return false

        val catalogId = resolveCatalog(input) ?: return false

        input.metadata?.let {
            importMetadata(catalogId, it)
        }

        input.media?.let {
            importMedia(catalogId, it)
        }

        return true
    }

    /** Validate every referenced media file before writing any import data. */
    private fun validateFiles(input: MediaImportV2): Boolean {
        val media = input.media
        val hasFiles = input.metadata?.cover != null ||
            media?.content != null ||
            !media?.subtitles.isNullOrEmpty()
        if (!hasFiles) return true

        val contentFolder = Env.getContentFolder()
        if (contentFolder == null || !contentFolder.isDirectory()) {
            log.warn { "Rejecting media import: CONTENT_FOLDER is not configured or is not a directory" }
            return false
        }

        val collection = input.reference?.store?.takeIf { it.isNotBlank() }
        if (collection == null) {
            log.warn { "Rejecting media import: collection is required to validate media paths" }
            return false
        }

        val root = contentFolder.toPath().toAbsolutePath().normalize()
        if (!contentFolder.exists()) {
            log.warn { "Rejecting media import: cannot access CONTENT_FOLDER '${contentFolder.path}'" }
            return false
        }

        val referencedFiles = buildList {
            input.metadata?.cover?.let { add(listOf(collection, it)) }
            media?.content?.let { add(listOf(collection, it.videoFile)) }
            media?.subtitles.orEmpty().forEach { add(listOf(collection, "sub", it.language, it.subtitleFile)) }
        }

        for (segments in referencedFiles) {
            if (segments.any { it.isBlank() || it == "." || it == ".." || '/' in it || '\\' in it }) {
                log.warn { "Rejecting media import: invalid media path segments ${segments.joinToString("/")}" }
                return false
            }

            val path = segments.fold(root) { current, segment -> current.resolve(segment) }.normalize()
            if (!path.startsWith(root)) {
                log.warn { "Rejecting media import: file resolves outside CONTENT_FOLDER: '$path'" }
                return false
            }
            val file: IFile = IFile(path.toString())
            if (!file.exists() || !file.isFile()) {
                log.warn { "Rejecting media import: file does not exist or is not a regular file: '$path'" }
                return false
            }
        }

        return true
    }

    private fun resolveCatalog(input: MediaImportV2): Long? {
        input.reference?.catalogId?.let {
            return it
        }

        val metadata = input.metadata
        val titles = listOfNotNull(metadata?.title) + metadata?.alternativeTitles.orEmpty()
        val matches = titleStore.findCollectionsByTitles(titles)
        if (matches.size == 1) {
            val matchedCollection = matches.single()
            log.info { "Reusing catalog ${matchedCollection.catalogId} matched by imported title" }
            return matchedCollection.catalogId
        }
        if (matches.size > 1) {
            log.error { "Import titles match multiple catalogs ${matches.map { it.catalogId }}; refusing to create another catalog" }
            return null
        }

        val newMetadata = metadata ?: return null
        val store = input.reference?.store ?: return null

        return catalogStore.insert(
            store = store,
            type = newMetadata.type,
            cover = newMetadata.cover
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
            val linkedVideoId = when (content.mediaType) {
                MediaType.Movie -> movieStore.getVideoId(catalogId)
                MediaType.Episode -> {
                    val episodeInfo = requireNotNull(content.episodeInfo)
                    serieStore.getVideoId(catalogId, episodeInfo.season, episodeInfo.episode)
                }
            }

            val linkedVideo = linkedVideoId?.let(videoStore::getById)
            val video = when {
                linkedVideo != null && linkedVideo.fileName != content.videoFile ->
                    videoStore.update(content.videoFile, linkedVideo)
                linkedVideo != null -> linkedVideo
                else -> videoStore.getByFile(content.videoFile)
                    ?: videoStore.insert(content.videoFile)
            }

            when (content.mediaType) {
                MediaType.Movie -> {
                    movieStore.insert(
                        catalogId = catalogId,
                        videoId = video.id
                    )
                }

                MediaType.Episode -> {
                    val episodeInfo = requireNotNull(content.episodeInfo)
                    serieStore.insert(
                        catalogId = catalogId,
                        videoId = video.id,
                        season = episodeInfo.season,
                        episode = episodeInfo.episode,
                        title = episodeInfo.title
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
