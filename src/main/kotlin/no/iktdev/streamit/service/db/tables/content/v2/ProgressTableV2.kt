package no.iktdev.streamit.service.db.tables.content.v2

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object ProgressTableV2 : LongIdTable(name = "PROGRESS") {
    val userId: Column<EntityID<Long>> =
        reference("USER_ID", UserTableV2, onDelete = ReferenceOption.CASCADE)

    val videoId: Column<EntityID<Long>> =
        reference("VIDEO_ID", VideoTableV2, onDelete = ReferenceOption.CASCADE)

    val position: Column<Long> =
        long("POSITION").default(0)


    val played: Column<LocalDateTime> = datetime("PLAYED")

    init {
        uniqueIndex(userId, videoId)
    }
}
