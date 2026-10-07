package no.iktdev.streamit.service.db.tables.content.v2

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object CatalogGenreTableV2 : Table(name = "CATALOG_GENRE") {
    val catalogId: Column<EntityID<Long>> = reference("CATALOG_ID", CatalogTableV2)
    val genreId: Column<EntityID<Long>> = reference("GENRE_ID", GenreTableV2)

    override val primaryKey = PrimaryKey(catalogId, genreId)
}