package no.iktdev.streamit.service.db.tables.content

import no.iktdev.streamit.service.db.tables.util.UpsertResult
import no.iktdev.streamit.service.db.tables.util.timestampToLocalDateTime
import no.iktdev.streamit.service.db.tables.util.withTransaction
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import kotlin.String


object ProgressTable : IntIdTable(name = "PROGRESS") {

    val guid: Column<String> = char("USER_ID", 36)
    val type: Column<String> = varchar("TYPE", 10)
    val title: Column<String> = varchar("TITLE", 250)
    val collection: Column<String?> = varchar("COLLECTION", 250).nullable()
    val episode: Column<Int?> = integer("EPISODE").nullable()
    val season: Column<Int?> = integer("SEASON").nullable()
    val video: Column<String> = varchar("VIDEO", 250)
    val progress: Column<Int> = integer("PROGRESS")
    val duration: Column<Int> = integer("DURATION")
    val played: Column<LocalDateTime?> = datetime("PLAYED").nullable()

    init {
        uniqueIndex("unique_progress_on_each_user", guid, title, collection, type, season, episode)
        index(true, guid, collection, season, episode)
    }

    fun selectUserRecords(userId: String): Query {
        return ProgressTable.selectAll()
            .where { guid eq userId }
    }

    /**
     * @return All progress record for user
     */
    fun selectRecords(userId: String, database: Database? = null, onError: ((Exception) -> Unit)? = null): List<ProgressTableObject> = withTransaction(database, onError) {
        selectUserRecords(userId)
            .where { guid eq userId }
            .map { ProgressTableObject.fromRow(it) }
    } ?: emptyList()

    fun selectMovieRecords(userId: String, database: Database? = null, onError: ((Exception) -> Unit)? = null): List<ProgressTableObject> = withTransaction(database, onError) {
        selectUserRecords(userId)
            .andWhere { type.eq("movie") }
            .map { ProgressTableObject.fromRow(it) }
    } ?: emptyList()

    fun selectSerieRecords(userId: String, database: Database? = null, onError: ((Exception) -> Unit)? = null): List<ProgressTableObject> = withTransaction(database, onError) {
        selectUserRecords(userId)
            .andWhere { type.eq("serie") }
            .map { ProgressTableObject.fromRow(it) }
    } ?: emptyList()

    fun selectSerieRecordsAfter(userId: String, time: Int, database: Database? = null, onError: ((Exception) -> Unit)? = null): List<ProgressTableObject> = withTransaction(database, onError) {
        selectUserRecords(userId)
            .andWhere { played.greater(timestampToLocalDateTime(time)) }
            .map { ProgressTableObject.fromRow(it) }
    } ?: emptyList()

    fun selectMovieRecordsAfter(userId: String, time: Int, database: Database? = null, onError: ((Exception) -> Unit)? = null): List<ProgressTableObject> = withTransaction(database, onError) {
        selectUserRecords(userId)
            .andWhere { played.greater(timestampToLocalDateTime(time)) }
            .map { ProgressTableObject.fromRow(it) }
    }
        ?: emptyList()
    fun selectMovieRecordOnTitle(userId: String, movieTitle: String, database: Database? = null, onError: ((Exception) -> Unit)? = null): List<ProgressTableObject> = withTransaction(database, onError) {
        selectUserRecords(userId)
            .andWhere { title.eq(movieTitle) }
            .map { ProgressTableObject.fromRow(it) }
    } ?: emptyList()

    fun selectSerieRecordOnCollection(userId: String, collection: String, database: Database? = null, onError: ((Exception) -> Unit)? = null): List<ProgressTableObject> = withTransaction(database, onError) {
        selectUserRecords(userId)
            .andWhere { ProgressTable.collection.eq(collection) }
            .orWhere { type.eq(collection) }
            .map { ProgressTableObject.fromRow(it) }
    } ?: emptyList()

    fun upsertMovieRecord(
        userId: String,
        title: String,
        collection: String,
        progress: Int,
        duration: Int,
        played: Int,
        videoFile: String
    ): UpsertResult {
        val trimmedTitle = title.trim()
        val playedTime = timestampToLocalDateTime(played)

        val insert = ProgressTable.insertIgnore {
            it[this.guid] = userId
            it[this.type] = "movie"
            it[this.title] = trimmedTitle
            it[this.progress] = progress
            it[this.duration] = duration
            it[this.collection] = collection
            it[this.played] = playedTime
            if (videoFile.isNotBlank()) {
                it[this.video] = videoFile
            }
        }

        return if (insert.insertedCount > 0) {
            UpsertResult.Inserted(insert)
        } else {
            val updatedRows = ProgressTable.update({
                (guid eq userId) and
                        (ProgressTable.title eq trimmedTitle) and
                        (type eq "movie")
            }) {
                it[this.progress] = progress
                it[this.duration] = duration
                it[this.played] = playedTime
                if (videoFile.isNotBlank()) {
                    it[this.video] = videoFile
                }
            }

            if (updatedRows > 0) UpsertResult.Updated else UpsertResult.Skipped
        }
    }

    fun upsertSerieRecord(
        userId: String,
        title: String,
        episode: Int,
        season: Int,
        collection: String,
        progress: Int,
        duration: Int,
        played: Int,
        videoFile: String
    ): UpsertResult {
        val trimmedTitle = title.trim()
        val playedTime = timestampToLocalDateTime(played)

        val insert = ProgressTable.insertIgnore {
            it[this.guid] = userId
            it[this.type] = "serie"
            it[this.title] = trimmedTitle
            it[this.episode] = episode
            it[this.season] = season
            it[this.progress] = progress
            it[this.duration] = duration
            it[this.collection] = collection
            it[this.played] = playedTime
            if (videoFile.isNotBlank()) {
                it[this.video] = videoFile
            }
        }

        return if (insert.insertedCount > 0) {
            UpsertResult.Inserted(insert)
        } else {
            val updatedRows = ProgressTable.update({
                (guid eq userId) and
                        (ProgressTable.title eq trimmedTitle) and
                        (type eq "serie") and
                        (ProgressTable.episode eq episode) and
                        (ProgressTable.season eq season)
            }) {
                it[this.progress] = progress
                it[this.duration] = duration
                it[this.played] = playedTime
                if (videoFile.isNotBlank()) {
                    it[this.video] = videoFile
                }
            }

            if (updatedRows > 0) UpsertResult.Updated else UpsertResult.Skipped
        }
    }

    fun selectLatestEpisodeWatched(guid: String): Query {
        return ProgressTable
            .join(
                SerieTable, JoinType.INNER,
                onColumn = ProgressTable.collection, otherColumn = SerieTable.collection
            )
            .join(
                ContinueWatchTable, JoinType.LEFT,
                onColumn = SerieTable.collection, otherColumn = ContinueWatchTable.collection
            ) {
                (ContinueWatchTable.userId eq guid) and
                        (ContinueWatchTable.type eq "serie")
            }
            .join(
                CatalogTable, JoinType.INNER,
                onColumn = SerieTable.collection, otherColumn = CatalogTable.collection
            )
            .select(
                ProgressTable.played,
                ProgressTable.progress,
                ProgressTable.duration,
                *CatalogTable.columns.toTypedArray(),
                *SerieTable.columns.toTypedArray()
            )
            .where {
                (ProgressTable.guid eq guid) and
                        (ProgressTable.type eq "serie") and
                        (ProgressTable.episode eq SerieTable.episode) and
                        (ProgressTable.season eq SerieTable.season) and
                        ((ContinueWatchTable.hide eq false) or ContinueWatchTable.hide.isNull())
            }
            .orderBy(ProgressTable.played, SortOrder.DESC)
    }

    fun selectResumeEpisode(guid: String, limit: Int, cutoffSeconds: Int = 5): Query {
        return selectLatestEpisodeWatched(guid)
            .andWhere { (progress lessEq (duration - cutoffSeconds)) }
            .limit(limit)
    }


    fun selectCompletedEpisodes(guid: String, limit: Int, cutoffSeconds: Int = 5): Query {
        return selectLatestEpisodeWatched(guid)
            .andWhere { (progress greater (duration - cutoffSeconds)) }
            .limit(limit)

    }

    fun selectNextEpisode(collection: String, currentSeason: Int, currentEpisode: Int): Query {
        return SerieTable.selectAll()
                .where {
                    (SerieTable.collection eq collection) and (
                            (SerieTable.season greater currentSeason) or
                                    ((SerieTable.season eq currentSeason) and (SerieTable.episode greater currentEpisode))
                            )
                }
                .orderBy(SerieTable.season, SortOrder.ASC)
                .orderBy(SerieTable.episode, SortOrder.ASC)
                .limit(1)
    }


    data class InternalProgressData(
        val collection: String,
        val season: Int,
        val episode: Int,
    )

}
