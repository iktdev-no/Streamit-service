package no.iktdev.streamit.service.db.tables.content

import no.iktdev.streamit.service.db.tables.util.withTransaction
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.selectAll

object GenreTable: IntIdTable(name = "GENRE") {
    val genre: Column<String> = varchar("GENRE", 50).uniqueIndex()


    fun selectById(id: Int): Query {
        return GenreTable.selectAll()
            .where{ GenreTable.id.eq(id) }
    }

    fun selectByIds(ids: List<Int>): Query {
        return GenreTable.selectAll()
            .where { id inList ids }
    }

    fun selectById(id: Int, database: Database? = null, onError: ((Exception) -> Unit)? = null): GenreTableObject? = withTransaction(database, onError) {
        GenreTable
            .selectAll()
            .where{ GenreTable.id.eq(id) }
            .map { GenreTableObject.fromRow(it) }
            .firstOrNull()
    }

    fun selectByIds(ids: List<Int>, database: Database? = null, onError: ((Exception) -> Unit)? = null): List<GenreTableObject> = withTransaction(database, onError) {
        GenreTable
            .selectAll()
            .where { id inList ids }
            .map { GenreTableObject.fromRow(it) }
    } ?: emptyList()
}