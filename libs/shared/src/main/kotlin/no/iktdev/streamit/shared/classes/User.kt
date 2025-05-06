package no.iktdev.streamit.shared.classes

import no.iktdev.streamit.library.db.tables.user.UserTable
import org.jetbrains.exposed.sql.ResultRow

data class User(val guid: String, val name: String, val image: String)
{
    companion object
    {
        fun fromRow(resultRow: ResultRow) = User(
            guid = resultRow[UserTable.guid],
            name = resultRow[UserTable.name],
            image = resultRow[UserTable.image]
        )
    }
}