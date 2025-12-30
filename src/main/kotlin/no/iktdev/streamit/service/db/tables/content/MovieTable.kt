package no.iktdev.streamit.service.db.tables.content

import no.iktdev.streamit.service.db.tables.util.withTransaction
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*

object MovieTable: IntIdTable(name = "MOVIE") {
    val video: Column<String> = varchar("VIDEO", 250).uniqueIndex()

    fun insertAndGetId(videoFile: String): EntityID<Int>? {
        return MovieTable.insertIgnoreAndGetId {
            it[video] = videoFile
        }
    }

    fun selectOnId(id: Int, database: Database? = null, onError: ((Exception) -> Unit)? = null): MovieTableObject? = withTransaction(database, onError) {
        MovieTable
            .selectAll()
            .where { MovieTable.id eq id }
            .map { MovieTableObject.fromRow(it) }
            .firstOrNull()
    }


    fun insertMovie(videoFile: String): EntityID<Int>? {
        return MovieTable.insertIgnoreAndGetId {
            it[video] = videoFile
        }
    }
}