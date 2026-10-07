package no.iktdev.streamit.service.stores.catalog

import no.iktdev.streamit.service.Env
import no.iktdev.streamit.service.db.tables.content.v2.CatalogGenreTableV2
import no.iktdev.streamit.service.db.tables.content.v2.CatalogTableV2
import no.iktdev.streamit.service.db.tables.content.v2.CatalogTitleTableV2
import no.iktdev.streamit.service.db.tables.content.v2.GenreTableV2
import no.iktdev.streamit.service.db.tables.content.v2.MovieTableV2
import no.iktdev.streamit.service.db.tables.content.v2.ProgressTableV2
import no.iktdev.streamit.service.db.tables.content.v2.SerieTableV2
import no.iktdev.streamit.service.db.tables.content.v2.SubtitleTableV2
import no.iktdev.streamit.service.db.tables.content.v2.UserTableV2
import no.iktdev.streamit.service.db.tables.content.v2.VideoTableV2
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.model.shared.content.Catalog
import no.iktdev.streamit.service.model.shared.content.ContentType
import no.iktdev.streamit.service.model.shared.content.Episode
import no.iktdev.streamit.service.model.shared.content.Genre
import no.iktdev.streamit.service.model.shared.content.Movie
import no.iktdev.streamit.service.model.shared.content.Progress
import no.iktdev.streamit.service.model.shared.content.Serie
import no.iktdev.streamit.service.model.shared.content.Subtitle
import no.iktdev.streamit.service.model.shared.content.Video
import no.iktdev.streamit.service.stores.user.IUserStore
import no.iktdev.streamit.service.stores.progress.toProgressInstant
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Component
class CatalogStore(
    private val userStore: IUserStore
) : ICatalogStore {
    override fun insert(
        store: String,
        type: ContentType,
        cover: String?
    ): Long {
        return withTransaction {
            CatalogTableV2.insertAndGetId {
                it[CatalogTableV2.store] = store
                it[CatalogTableV2.type] = type
                it[CatalogTableV2.cover] = cover
            }.value
        }.getOrThrow()
    }

    override fun updateCover(
        id: Long,
        cover: String
    ): Boolean {
        return withTransaction {
            CatalogTableV2.update(
                where = { CatalogTableV2.id eq id }
            ) {
                it[CatalogTableV2.cover] = cover
            } > 0
        }.isSuccess
    }

    override fun getAll(
        offset: Long,
        limit: Int?
    ): List<Catalog> =
        getCatalogs(offset, limit) {
            CatalogTitleTableV2.preferred eq true
        }

    override fun getByIds(ids: List<Long>): List<Catalog> {
        return getCatalogs(
            predicate = {
                CatalogTableV2.id inList ids.map {
                    EntityID(it, CatalogTableV2)
                }
            }
        )
    }

    override fun getMovies(
        offset: Long,
        limit: Int
    ): List<Catalog> =
        getCatalogs(offset, limit) {
            (CatalogTitleTableV2.preferred eq true) and
                    (CatalogTableV2.type eq ContentType.Movie)
        }

    override fun getSeries(
        offset: Long,
        limit: Int
    ): List<Catalog> =
        getCatalogs(offset, limit) {
            (CatalogTitleTableV2.preferred eq true) and
                    (CatalogTableV2.type eq ContentType.Serie)
        }


    override fun getMovieById(id: Long): Movie? {
        return withTransaction {
            val movie = getMovie {
                (CatalogTableV2.id eq id) and
                        (CatalogTableV2.type eq ContentType.Movie)
            }
            movie
        }.getOrNull()
    }

    override fun getSerieById(id: Long): Serie? {
        return withTransaction {
            val serie = getSerie {
                (CatalogTableV2.id eq id) and
                        (CatalogTableV2.type eq ContentType.Serie)
            }
            serie
        }.getOrNull()
    }

    override fun getSerieByStore(store: String): Serie? = withTransaction {
        getSerie {
            (CatalogTableV2.store eq store) and
                    (CatalogTableV2.type eq ContentType.Serie)
        }
    }.getOrNull()


    override fun getByGenre(
        genreId: Long,
        offset: Long,
        limit: Int
    ): List<Catalog> =
        getCatalogs(offset, limit) {
            (CatalogTitleTableV2.preferred eq true) and
                    exists(
                        CatalogGenreTableV2
                            .selectAll()
                            .where {
                                (CatalogGenreTableV2.catalogId eq CatalogTableV2.id) and
                                        (CatalogGenreTableV2.genreId eq genreId)
                            }
                    )
        }

    override fun getRecentlyAdded(freshness: Long): List<Catalog> =
        getCatalogs(
            offset = 0,
            limit = freshness.toInt(),
            orderBy = CatalogTableV2.addedAt to SortOrder.DESC
        ) {
            CatalogTitleTableV2.preferred eq true
        }

    override fun getRecentlyUpdatedSeries(): List<Catalog> =
        getCatalogs(0, 100) {
            (CatalogTableV2.type eq ContentType.Serie) and
                    (CatalogTitleTableV2.preferred eq true) and
                    (CatalogTableV2.updatedAt greaterEq Env.getSerieCutoff())
        }

    override fun getContinueOrResumeSerie(userId: UUID): List<Serie> {
        TODO("Not yet implemented")
    }

    override fun search(
        keyword: String
    ): List<Catalog> =
        getCatalogs(0, 100) {
            CatalogTitleTableV2.title.lowerCase() like "%${keyword.lowercase()}%"
        }

    override fun searchMovies(keyword: String): List<Catalog> {
        return getCatalogs(0, 100) {
            (CatalogTitleTableV2.title.lowerCase() like "%${keyword.lowercase()}%")
                .and (CatalogTableV2.type eq ContentType.Movie)
        }
    }

    override fun searchSeries(keyword: String): List<Catalog> {
        return getCatalogs(0, 100) {
            (CatalogTitleTableV2.title.lowerCase() like "%${keyword.lowercase()}%")
                .and (CatalogTableV2.type eq ContentType.Serie)
        }
    }

    override fun getMovieByIdWithProgress(
        id: Long,
        userId: UUID
    ): Movie? {
        val internalUserId = userStore.getByUid(userId)?.id
            ?: return null

        return withTransaction {
            getMovie(internalUserId) {
                (CatalogTableV2.id eq id) and
                        (CatalogTableV2.type eq ContentType.Movie)
            }
        }.getOrNull()
    }

    override fun getSerieByIdWithProgress(
        id: Long,
        userId: UUID
    ): Serie? {
        val internalUserId = userStore.getByUid(userId)?.id
            ?: return null

        return withTransaction {
            getSerie(internalUserId) {
                (CatalogTableV2.id eq id) and
                        (CatalogTableV2.type eq ContentType.Serie)
            }
        }.getOrNull()
    }

    private fun getCatalogs(
        offset: Long? = null,
        limit: Int? = null,
        orderBy: Pair<Expression<*>, SortOrder> = CatalogTableV2.id to SortOrder.ASC,
        predicate: SqlExpressionBuilder.() -> Op<Boolean>,
    ): List<Catalog> = withTransaction {
        val query = CatalogTableV2
            .innerJoin(CatalogTitleTableV2)
            .selectAll()
            .where(predicate)
            .orderBy(orderBy)

        limit?.let { query.limit(it) }
        offset?.let { query.offset(it) }

        val catalogs = query.map { row ->
            Catalog(
                id = row[CatalogTableV2.id].value,
                title = row[CatalogTitleTableV2.title],
                cover = row[CatalogTableV2.cover],
                type = row[CatalogTableV2.type],
                collection = row[CatalogTableV2.store],
                recent = row[CatalogTableV2.updatedAt]
                    .isAfter(LocalDateTime.now().minusDays(Env.frshness))
            )
        }

        if (catalogs.isEmpty()) {
            return@withTransaction emptyList()
        }

        val catalogIds = catalogs.map {
            EntityID(it.id, CatalogTableV2)
        }

        val genreRows = CatalogGenreTableV2
            .innerJoin(GenreTableV2)
            .selectAll()
            .where {
                CatalogGenreTableV2.catalogId inList catalogIds
            }
            .groupBy {
                it[CatalogGenreTableV2.catalogId].value
            }

        catalogs.forEach { catalog ->
            catalog.genres = genreRows[catalog.id]
                ?.map {
                    Genre(
                        id = it[GenreTableV2.id].value.toInt(),
                        genre = it[GenreTableV2.genre]
                    )
                }
                .orEmpty()
        }

        catalogs
    }.getOrDefault(emptyList())

    private fun getMovie(
        userId: Long? = null,
        predicate: SqlExpressionBuilder.() -> Op<Boolean>
    ): Movie? {
        val query = CatalogTableV2
            .innerJoin(CatalogTitleTableV2)
            .innerJoin(MovieTableV2)
            .innerJoin(VideoTableV2)
            .let {
                if (userId != null) {
                    it.leftJoin(
                        ProgressTableV2,
                        additionalConstraint = {
                            (ProgressTableV2.videoId eq VideoTableV2.id) and
                                    (ProgressTableV2.userId eq EntityID(userId, UserTableV2))
                        }
                    )
                } else {
                    it
                }
            }

        val row = query
            .selectAll()
            .where {
                (CatalogTitleTableV2.preferred eq true) and predicate()
            }
            .singleOrNull()
            ?: return null

        val catalogId = row[CatalogTableV2.id]
        val videoId = row[VideoTableV2.id]

        val progress = if (userId != null) {
            row.getOrNull(ProgressTableV2.position)?.let {
                Progress(
                    position = it,
                    played = row[ProgressTableV2.played].toProgressInstant()
                )
            }
        } else {
            null
        }

        return Movie(
            id = catalogId.value,
            title = row[CatalogTitleTableV2.title],
            cover = row[CatalogTableV2.cover],
            collection = row[CatalogTableV2.store],
            recent = row[CatalogTableV2.updatedAt]
                .isAfter(LocalDateTime.now().minusDays(Env.frshness)),
            video = Video(
                id = videoId.value,
                videoFileName = row[VideoTableV2.file],
                duration = row[VideoTableV2.duration] ?: 0
            ),
            genres = getGenres(listOf(catalogId))[catalogId.value].orEmpty(),
            subtitles = getSubtitles(listOf(videoId))[videoId.value].orEmpty(),
            progress = progress
        )
    }

    private fun getSerie(
        userId: Long? = null,
        predicate: SqlExpressionBuilder.() -> Op<Boolean>
    ): Serie? {
        val query = CatalogTableV2
            .innerJoin(CatalogTitleTableV2)
            .innerJoin(SerieTableV2)
            .innerJoin(VideoTableV2)
            .let {
                if (userId != null) {
                    it.leftJoin(
                        ProgressTableV2,
                        additionalConstraint = {
                            (ProgressTableV2.videoId eq VideoTableV2.id) and
                                    (ProgressTableV2.userId eq EntityID(userId, UserTableV2))
                        }
                    )
                } else {
                    it
                }
            }

        val row = query
            .selectAll()
            .where {
                (CatalogTitleTableV2.preferred eq true) and predicate()
            }
            .singleOrNull()
            ?: return null

        val catalogId = row[CatalogTableV2.id]
        val videoId = row[VideoTableV2.id]

        val progress = if (userId != null) {
            row.getOrNull(ProgressTableV2.position)?.let {
                Progress(
                    position = it,
                    played = row[ProgressTableV2.played].toProgressInstant()
                )
            }
        } else {
            null
        }

        val episode = Episode(
            season = row[SerieTableV2.season],
            episode = row[SerieTableV2.episode],
            title = row[SerieTableV2.title],
            video = Video(
                id = videoId.value,
                videoFileName = row[VideoTableV2.file],
                duration = row[VideoTableV2.duration] ?: 0
            ),
            subtitles = getSubtitles(listOf(videoId))[videoId.value].orEmpty(),
            progress = progress
        )

        return Serie(
            id = catalogId.value,
            title = row[CatalogTitleTableV2.title],
            cover = row[CatalogTableV2.cover],
            collection = row[CatalogTableV2.store],
            genres = getGenres(listOf(catalogId))[catalogId.value].orEmpty(),
            recent = row[CatalogTableV2.updatedAt]
                .isAfter(LocalDateTime.now().minusDays(Env.frshness)),
            episodes = listOf(episode)
        )
    }

    private fun getGenres(
        catalogIds: List<EntityID<Long>>
    ): Map<Long, List<Genre>> {
        if (catalogIds.isEmpty()) return emptyMap()

        return CatalogGenreTableV2
            .innerJoin(GenreTableV2)
            .selectAll()
            .where {
                CatalogGenreTableV2.catalogId inList catalogIds
            }
            .groupBy {
                it[CatalogGenreTableV2.catalogId].value
            }
            .mapValues { (_, rows) ->
                rows.map {
                    Genre(
                        id = it[GenreTableV2.id].value.toInt(),
                        genre = it[GenreTableV2.genre]
                    )
                }
            }
    }

    private fun getSubtitles(
        videoIds: List<EntityID<Long>>
    ): Map<Long, List<Subtitle>> {
        if (videoIds.isEmpty()) return emptyMap()

        return SubtitleTableV2
            .selectAll()
            .where {
                SubtitleTableV2.videoId inList videoIds
            }
            .groupBy {
                it[SubtitleTableV2.videoId].value
            }
            .mapValues { (_, rows) ->
                rows.map {
                    Subtitle(
                        id = it[SubtitleTableV2.id].value.toInt(),
                        language = it[SubtitleTableV2.language],
                        format = it[SubtitleTableV2.format],
                        file = it[SubtitleTableV2.file]
                    )
                }
            }
    }
}
