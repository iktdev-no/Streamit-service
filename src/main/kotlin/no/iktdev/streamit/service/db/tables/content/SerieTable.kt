package no.iktdev.streamit.service.db.tables.content

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.time.LocalDateTime

object SerieTable: IntIdTable(name = "SERIE") {
    val title: Column<String?> = varchar("TITLE", 250).nullable()
    val episode: Column<Int> = integer("EPISODE")
    val season: Column<Int> = integer("SEASON")
    val collection: Column<String> = varchar("COLLECTION", 250)
    val video: Column<String> = varchar("VIDEO", 500).uniqueIndex()
    val added: Column<LocalDateTime> = datetime("ADDED_AT").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(episode, season, collection)
    }

    fun insertIgnore(title: String, collection: String, episode: Int, season: Int, videoFile: String): InsertStatement<Long> {
        return SerieTable.insertIgnore {
            it[SerieTable.title] = title
            it[SerieTable.episode] = episode
            it[SerieTable.season] = season
            it[SerieTable.collection] = collection
            it[video] = videoFile
        }
    }

    fun insertSerie(title: String, collection: String, episode: Int, season: Int, videoFile: String): EntityID<Int>? {
        return SerieTable.insertIgnoreAndGetId {
            it[this.title] = title
            it[this.collection] = collection
            it[this.episode] = episode
            it[this.season] = season
            it[this.video] = videoFile
            it[this.added] = LocalDateTime.now()
        }
    }

    fun selectOnCollection(collection: String): Query {
        return CatalogTable.join(SerieTable, JoinType.INNER) {
            CatalogTable.collection eq SerieTable.collection
        }
            .selectAll()
            .where { CatalogTable.collection eq collection }
            .orderBy(season)
            .orderBy(episode)
            .andWhere { CatalogTable.collection.isNotNull() }
            .andWhere { CatalogTable.type.eq("serie") }
    }

    fun selectOnId(id: Int): Query {
        return CatalogTable.join(SerieTable, JoinType.INNER) {
            CatalogTable.collection eq collection
        }
            .selectAll()
            .where { CatalogTable.id eq id }
            .orderBy(season)
            .orderBy(episode)
            .andWhere { CatalogTable.collection.isNotNull() }
            .andWhere { CatalogTable.type.eq("serie") }
    }
}
