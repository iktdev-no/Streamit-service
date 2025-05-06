package no.iktdev.streamit.shared.classes

import no.iktdev.streamit.library.db.tables.content.ProgressTable
import no.iktdev.streamit.shared.database.toEpochSeconds
import org.jetbrains.exposed.sql.ResultRow

data class ProgressTableDto(
    val id: Int,
    val guid: String,
    val type: String,
    val title: String,
    val collection: String,
    val episode: Int?,
    val season: Int?,
    val video: String, // Might not be recorded
    val progress: Int,
    val duration: Int,
    val played: Int
) {
    companion object {
        fun fromRow(resultRow: ResultRow) = ProgressTableDto(
            id = resultRow[ProgressTable.id].value,
            guid = resultRow[ProgressTable.guid],
            type = resultRow[ProgressTable.type],
            title = resultRow[ProgressTable.title],
            collection = resultRow[ProgressTable.collection] ?: resultRow[ProgressTable.title],
            episode = resultRow[ProgressTable.episode],
            season = resultRow[ProgressTable.season],
            video = resultRow[ProgressTable.video],
            progress = resultRow[ProgressTable.progress],
            duration = resultRow[ProgressTable.duration],
            played = resultRow[ProgressTable.played]?.toEpochSeconds()?.toInt() ?: 0,
        )

        fun fromSerieToList(userId: String, serie: Serie): List<ProgressTableDto> = serie.episodes.map {
            ProgressTableDto(
                id = serie.id,
                guid = userId,
                type = "serie",
                title = serie.title,
                collection = serie.collection,
                video = it.video,
                season = it.season,
                episode = it.episode,
                progress = it.progress,
                duration = it.duration,
                played = it.played
            )
        }
    }
}

fun ProgressTableDto.isMovie(): Boolean {
    return this.type.lowercase() == "movie";
}

fun ProgressTableDto.isSerie(): Boolean {
    return this.type.lowercase() == "serie";
}

abstract class BaseProgress {
    abstract val guid: String
    abstract val type: ContentType
    abstract val title: String
    abstract val collection: String

}

data class ProgressMovie(
    override val guid: String,
    override val title: String,
    override val type: ContentType,
    override val collection: String,
    val progress: Int,
    val duration: Int,
    var played: Int,
    val video: String?

) : BaseProgress() {
    companion object {
        fun fromProgressTable(item: ProgressTableDto) = ProgressMovie(
            guid = item.guid,
            title = item.title,
            type = ContentType.Movie,
            video = item.video,
            collection = item.collection,
            progress = item.progress,
            duration = item.duration,
            played = item.played
        )

        fun fromRow(resultRow: ResultRow) = ProgressMovie(
            guid = resultRow[ProgressTable.guid],
            title = resultRow[ProgressTable.title],
            type = ContentType.Movie,
            video = resultRow[ProgressTable.video],
            progress = resultRow[ProgressTable.progress],
            duration = resultRow[ProgressTable.duration],
            played = resultRow[ProgressTable.played]?.toEpochSeconds()?.toInt() ?: 0,
            collection = resultRow[ProgressTable.collection] ?: resultRow[ProgressTable.title],
        )
    }
}

data class ProgressSerie(
    override val guid: String,
    override val type: ContentType,
    override val title: String,
    override val collection: String,
    var episodes: List<ProgressEpisode> = emptyList(),
) : BaseProgress() {
    companion object {
        fun fromProgressTable(item: ProgressTableDto) = ProgressSerie(
            guid = item.guid,
            title = item.title,
            type = ContentType.Serie,
            collection = item.collection,
            episodes = listOf()
        )
    }
}

data class ProgressEpisode(
    val season: Int,
    val episode: Int,
    val progress: Int,
    val duration: Int,
    val played: Int,
    val video: String?
) {
    companion object {
        fun fromFlat(item: ProgressTableDto): ProgressEpisode? {
            val seasonNumber = item.season ?: return null
            val episodeNumber = item.episode ?: return null

            return ProgressEpisode(
                season = seasonNumber,
                episode = episodeNumber,
                video = item.video,
                progress = item.progress,
                duration = item.duration,
                played = item.played
            )

        }
    }
}