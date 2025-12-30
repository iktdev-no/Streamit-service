package no.iktdev.streamit.service.db.tables.content

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction

object FavoriteTable: IntIdTable(name = "FAVORITES") {
    val userId: Column<String> = varchar("USER_ID", 50)
    val catalogId: Column<Int> = integer("CATALOG_ID")

    init {
        uniqueIndex(userId, catalogId)
    }

    fun addFavorite(userId: String, catalogId: Int): Boolean = transaction {
        FavoriteTable.insertIgnore {
            it[FavoriteTable.userId] = userId
            it[FavoriteTable.catalogId] = catalogId
        }.insertedCount != 0
    }

    fun removeFavorite(userId: String, catalogId: Int): Boolean = transaction {
        FavoriteTable.deleteWhere { (FavoriteTable.userId eq userId) and (FavoriteTable.catalogId eq catalogId) } != 0
    }

    fun getFavorites(userId: String): List<Int> = transaction {
        FavoriteTable.select(catalogId).where { (FavoriteTable.userId eq userId) }
            .map { it -> it[catalogId] }
    }

}