package no.iktdev.streamit.service.stores.favorite

import no.iktdev.streamit.service.db.tables.content.v2.CatalogTableV2
import no.iktdev.streamit.service.db.tables.content.v2.FavoritesTableV2
import no.iktdev.streamit.service.db.tables.content.v2.UserTableV2
import no.iktdev.streamit.service.db.tables.util.withTransaction
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class FavoriteStore : IFavoriteStore {

    override fun getAll(userId: String): List<Long> =
        withTransaction {
            FavoritesTableV2
                .innerJoin(UserTableV2)
                .select(FavoritesTableV2.catalogId)
                .where {
                    UserTableV2.uid eq userId
                }
                .map {
                    it[FavoritesTableV2.catalogId].value
                }
        }.getOrElse { emptyList() }

    override fun add(userId: String, catalogId: Long) {
        withTransaction {
            val user = UserTableV2
                .select(UserTableV2.id)
                .where {
                    UserTableV2.uid eq userId
                }
                .singleOrNull()
                ?: return@withTransaction

            FavoritesTableV2.insertIgnore {
                it[FavoritesTableV2.userId] = user[UserTableV2.id]
                it[FavoritesTableV2.catalogId] = catalogId
            }
        }.getOrThrow()
    }

    override fun remove(userId: String, catalogId: Long) {
        withTransaction {
            val user = UserTableV2
                .select(UserTableV2.id)
                .where {
                    UserTableV2.uid eq userId
                }
                .singleOrNull()
                ?: return@withTransaction

            FavoritesTableV2.deleteWhere {
                (FavoritesTableV2.userId eq user[UserTableV2.id]) and
                        (FavoritesTableV2.catalogId eq catalogId)
            }
        }.getOrThrow()
    }
}