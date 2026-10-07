package no.iktdev.streamit.service

import no.iktdev.streamit.service.db.tables.content.v2.*
import no.iktdev.streamit.service.model.shared.content.ContentType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/** Minimal relational fixtures for the V2 catalog schema. */
object TestContentData {
    data class CatalogFixture(val catalogId: Long, val videoId: Long? = null, val genreId: Long? = null)

    fun movie(title: String, store: String = title, videoFile: String = "$title.mp4", genre: String? = null): CatalogFixture = transaction {
        catalog(store, title, ContentType.Movie, videoFile, genre, null, null)
    }

    fun episode(
        title: String,
        store: String,
        videoFile: String,
        season: Int,
        episode: Int,
        genre: String? = null
    ): CatalogFixture = transaction {
        catalog(store, title, ContentType.Serie, videoFile, genre, season, episode)
    }

    private fun catalog(
        store: String,
        title: String,
        type: ContentType,
        videoFile: String?,
        genre: String?,
        season: Int?,
        episode: Int?
    ): CatalogFixture {
        val catalogId = CatalogTableV2.insertAndGetId {
            it[CatalogTableV2.store] = store
            it[CatalogTableV2.type] = type
            it[CatalogTableV2.cover] = "$title.jpg"
        }.value
        CatalogTitleTableV2.insert {
            it[CatalogTitleTableV2.catalogId] = catalogId
            it[CatalogTitleTableV2.title] = title
            it[CatalogTitleTableV2.language] = "eng"
            it[CatalogTitleTableV2.preferred] = true
        }

        val genreId = genre?.let { name ->
            GenreTableV2.insertIgnore { it[GenreTableV2.genre] = name }
            GenreTableV2.selectAll().where { GenreTableV2.genre eq name }.single()[GenreTableV2.id].value
        }
        if (genreId != null) {
            CatalogGenreTableV2.insertIgnore {
                it[CatalogGenreTableV2.catalogId] = catalogId
                it[CatalogGenreTableV2.genreId] = genreId
            }
        }

        val videoId = videoFile?.let { file ->
            VideoTableV2.insertAndGetId { it[VideoTableV2.file] = file }.value
        }
        if (videoId != null) {
            if (season == null || episode == null) {
                MovieTableV2.insert {
                    it[MovieTableV2.catalogId] = catalogId
                    it[MovieTableV2.videoId] = videoId
                }
            } else {
                SerieTableV2.insert {
                    it[SerieTableV2.catalogId] = catalogId
                    it[SerieTableV2.videoId] = videoId
                    it[SerieTableV2.season] = season
                    it[SerieTableV2.episode] = episode
                    it[SerieTableV2.title] = null
                }
            }
        }
        return CatalogFixture(catalogId, videoId, genreId)
    }
}
