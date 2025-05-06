package no.iktdev.streamit.shared.database.queries

import no.iktdev.streamit.shared.classes.User
import no.iktdev.streamit.library.db.tables.user.UserTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun UserTable.executeSelectAll(): List<User> {
    return transaction {
        this@executeSelectAll.selectAll().mapNotNull { User.fromRow(it) }
    }
}
fun UserTable.executeSelectWith(id: String): User? {
    return transaction {
        this@executeSelectWith.selectUser(id).firstNotNullOfOrNull { User.fromRow(it) }
    }
}

fun UserTable.upsert(user: User) {
    val present = UserTable.executeSelectWith(user.guid)
    transaction {
        if (present != null) {
            this@upsert.update({ guid eq user.guid}) {
                it[name] = user.name
                it[image] = user.image
            }
        } else {
            this@upsert.insert {
                it[guid] = user.guid
                it[name] = user.name
                it[image] = user.image
            }
        }
    }
}

fun UserTable.executeDeleteWith(id: String): Boolean {
    return transaction {
        this@executeDeleteWith.deleteWhere { guid eq id }
    } != 0
}