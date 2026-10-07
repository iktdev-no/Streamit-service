package no.iktdev.streamit.service.db.tables.content.v2

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
object SerieTableV2 : LongIdTable(name = "SERIE") {
    val catalogId: Column<EntityID<Long>> = reference("CATALOG_ID", CatalogTableV2)
    val episode: Column<Int> = integer("EPISODE")
    val season: Column<Int> = integer("SEASON")
    val title: Column<String?> = varchar("TITLE", 500).nullable()
    val videoId: Column<EntityID<Long>> = reference("VIDEO_ID", VideoTableV2)
    val addedAt: Column<LocalDateTime> = datetime("ADDED_AT")

    init {
        uniqueIndex(catalogId, episode, season)
        uniqueIndex(videoId)
    }
}