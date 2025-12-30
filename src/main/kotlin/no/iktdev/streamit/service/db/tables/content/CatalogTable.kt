package no.iktdev.streamit.service.db.tables.content

import no.iktdev.streamit.service.db.tables.util.withTransaction
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.time.LocalDateTime

object CatalogTable : IntIdTable(name = "CATALOG") {
    val title: Column<String> = varchar("TITLE", 250)
    var cover: Column<String?> = varchar("COVER", 250).nullable()
    var type: Column<String> = varchar("TYPE", 50)
    var collection: Column<String> = varchar("COLLECTION", 250)
    var iid: Column<Int?> = integer("IID").nullable()
    var genres: Column<String?> = varchar("GENRES", 24).nullable()
    val added: Column<LocalDateTime> = datetime("ADDED").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(title, type)
    }
    fun selectOnlyMovies(database: Database? = null, onError: ((Exception) -> Unit)? = null): List<CatalogTableObject> = withTransaction(database, onError) {
        CatalogTable
            .selectAll()
            .where { type eq "movie" }
            .andWhere { collection.isNotNull() }
            .map { CatalogTableObject.fromRow(it) }
    } ?: emptyList()


    fun selectOnlySeries(database: Database? = null, onError: ((Exception) -> Unit)? = null): List<CatalogTableObject> = withTransaction(database, onError) {
        CatalogTable.selectAll()
            .where { type eq "serie" }
            .andWhere { collection.isNotNull() }
            .map { CatalogTableObject.fromRow(it) }
    } ?: emptyList()

    fun selectWhereGenreIsSet(database: Database? = null, onError: ((Exception) -> Unit)? = null): List<CatalogTableObject> = withTransaction(database, onError) {
        CatalogTable.selectAll()
            .where {genres.isNotNull() }
            .map { CatalogTableObject.fromRow(it) }
    } ?: emptyList()

    fun selectRecentlyAdded(limit: Int, database: Database? = null, onError: ((Exception) -> Unit)? = null): List<CatalogTableObject> = withTransaction(database, onError) {
        CatalogTable.selectAll()
            .orderBy(id, SortOrder.DESC)
            .limit(limit)
            .map { CatalogTableObject.fromRow(it) }
    } ?: emptyList()

    /**
     * @param noOlderThan Defines the final cutoff for content to be shown
     */
    fun selectNewlyUpdatedSeries(noOlderThan: LocalDateTime, database: Database? = null, onError: ((Exception) -> Unit)? = null): List<CatalogTableRecentlyUpdatedSeriesObject> = withTransaction(database, onError) {
        val episodeAddedAlias = SerieTable.added.max().alias("episodeAdded")
        val latestEpisode = SerieTable
            .select(SerieTable.collection, episodeAddedAlias)
            .groupBy(SerieTable.collection)
            .alias("UpdatedEpisodeTable")

        join(latestEpisode, JoinType.INNER) {
            collection eq latestEpisode[SerieTable.collection]
        }
            .selectAll()
            .where { type eq "serie" }
            .andWhere { latestEpisode[episodeAddedAlias].greater(noOlderThan) }
            .orderBy(latestEpisode[episodeAddedAlias], SortOrder.DESC)
            .map { row ->
                CatalogTableRecentlyUpdatedSeriesObject(
                    id = row[id].value,
                    title = row[title],
                    cover = row[cover],
                    type = row[type],
                    collection = row[collection],
                    iid = row[iid],
                    genres = row[genres],
                    added = row[added],
                    serieTableEntryAdded = row[latestEpisode[episodeAddedAlias]] ?: LocalDateTime.MIN
                )
            }
    } ?: emptyList()


    fun insertMovie(title: String, collection: String, cover: String? = null, genres: String? = null, videoFile: String): InsertStatement<Number>? {
        val inserted = MovieTable.insertMovie(videoFile)?.value ?: return null

        return CatalogTable.insert {
            it[iid] = inserted
            it[CatalogTable.title] = title
            it[CatalogTable.collection] = collection
            it[CatalogTable.cover] = cover
            it[CatalogTable.type] = "movie"
            it[CatalogTable.genres] = genres
            it[CatalogTable.added] = LocalDateTime.now()
        }

    }

    fun insertSerie(title: String, collection: String, cover: String? = null, genres: String? = null, videoFile: String, episode: Int, season: Int): InsertStatement<Long>? {
        val inserted = SerieTable.insertSerie(title, collection, episode, season, videoFile)?.value ?: return null

        return CatalogTable.insertIgnore {
            it[iid] = inserted
            it[CatalogTable.title] = title
            it[CatalogTable.collection] = collection
            it[CatalogTable.cover] = cover
            it[CatalogTable.type] = "serie"
            it[CatalogTable.genres] = genres
            it[CatalogTable.added] = LocalDateTime.now()
        }

    }


    fun searchMovieWith(keyword: String, database: Database? = null, onError: ((Exception) -> Unit)? = null): List<CatalogTableObject> = withTransaction(database, onError) {
        CatalogTable
            .selectAll()
            .where { type eq "movie" }
            .andWhere { collection.isNotNull() }
            .andWhere { this@CatalogTable.title like "$keyword%" }
            .orWhere { this@CatalogTable.collection like "%$keyword%" }
            .map { CatalogTableObject.fromRow(it) }
    } ?: emptyList()

    fun searchSerieWith(keyword: String, database: Database? = null, onError: ((Exception) -> Unit)? = null): List<CatalogTableObject> = withTransaction(database, onError) {
        CatalogTable.selectAll()
            .where { type eq "serie" }
            .andWhere { collection.isNotNull() }
            .andWhere { this@CatalogTable.title like "$keyword%" }
            .orWhere { this@CatalogTable.collection like "%$keyword%" }
            .map { CatalogTableObject.fromRow(it) }
    } ?: emptyList()

    fun searchWith(keyword: String, database: Database? = null, onError: ((Exception) -> Unit)? = null): List<CatalogTableObject> = withTransaction(database, onError) {
        CatalogTable.selectAll()
            .where { this@CatalogTable.title like "$keyword%" }
            .orWhere { this@CatalogTable.collection like "%$keyword%" }
            .map { CatalogTableObject.fromRow(it) }
    } ?: emptyList()


}