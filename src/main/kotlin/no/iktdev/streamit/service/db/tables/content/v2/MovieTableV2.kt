package no.iktdev.streamit.service.db.tables.content.v2

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
object MovieTableV2 : LongIdTable(name = "MOVIE") {
    val catalogId: Column<EntityID<Long>> = reference("CATALOG_ID", CatalogTableV2)
    val videoId: Column<EntityID<Long>> = reference("VIDEO_ID", VideoTableV2)
}