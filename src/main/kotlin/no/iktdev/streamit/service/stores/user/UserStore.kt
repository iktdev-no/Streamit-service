package no.iktdev.streamit.service.stores.user

import no.iktdev.streamit.service.db.tables.content.v2.UserTableV2
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.model.internal.user.UserData
import no.iktdev.streamit.service.model.internal.user.User
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserStore : IUserStore {

    override fun get(id: Long): User? = withTransaction {
        UserTableV2
            .selectAll()
            .where { UserTableV2.id eq id }
            .singleOrNull()
            ?.toUser()
    }.getOrNull()

    override fun getByUid(uid: UUID): User? = withTransaction {
        UserTableV2
            .selectAll()
            .where { UserTableV2.uid eq uid.toString() }
            .singleOrNull()
            ?.toUser()
    }.getOrNull()

    override fun getAll(): List<User> = withTransaction {
        UserTableV2
            .selectAll()
            .orderBy(UserTableV2.name to SortOrder.ASC)
            .map { it.toUser() }
    }.getOrDefault(emptyList())

    override fun insert(user: UserData): User =
        withTransaction {
            val id = UserTableV2.insertAndGetId {
                it[UserTableV2.uid] = UUID.randomUUID().toString()
                it[UserTableV2.name] = user.name
                it[UserTableV2.image] = user.image
            }

            UserTableV2
                .selectAll()
                .where { UserTableV2.id eq id }
                .single()
                .toUser()
        }.getOrThrow()

    override fun update(
        uid: UUID,
        name: String,
        image: String
    ): User? = withTransaction {
        val updated = UserTableV2.update(
            where = { UserTableV2.uid eq uid.toString() }
        ) {
            it[UserTableV2.name] = name
            it[UserTableV2.image] = image
        }

        if (updated == 0) {
            null
        } else {
            UserTableV2
                .selectAll()
                .where { UserTableV2.uid eq uid.toString() }
                .single()
                .toUser()
        }
    }.getOrThrow()

    override fun delete(uid: UUID): Boolean {
        return withTransaction {
            UserTableV2.deleteWhere {
                UserTableV2.uid eq uid.toString()
            }
        }.isSuccess
    }

    private fun ResultRow.toUser(): User =
        User(
            id = this[UserTableV2.id].value,
            uid = this[UserTableV2.uid],
            name = this[UserTableV2.name],
            image = this[UserTableV2.image]
        )
}