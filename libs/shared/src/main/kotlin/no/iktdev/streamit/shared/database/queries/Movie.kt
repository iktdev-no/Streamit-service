package no.iktdev.streamit.shared.database.queries

import no.iktdev.streamit.shared.classes.Movie
import no.iktdev.streamit.shared.classes.Subtitle
import no.iktdev.streamit.library.db.tables.content.CatalogTable
import no.iktdev.streamit.library.db.tables.content.GenreTable
import no.iktdev.streamit.library.db.tables.content.MovieTable
import no.iktdev.streamit.library.db.tables.content.SubtitleTable
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

fun MovieTable.executeSelectOnId(id: Int): Movie? {
    return transaction {
        val movie = CatalogTable.innerJoin(MovieTable, { iid }, { MovieTable.id })
            .select { CatalogTable.id eq id }
            .andWhere { CatalogTable.iid.isNotNull() }
            .map { Movie.fromRow(it) }.singleOrNull()
        movie?.video?.let { File(it).nameWithoutExtension }?.let { videoName ->
            movie.subtitles = SubtitleTable.select { SubtitleTable.associatedWithVideo eq videoName }
                .map { Subtitle.fromRow(it) }
        }
        movie?.genres?.let {
            val ids = it.split(",").mapNotNull { g -> g.toIntOrNull() }
            GenreTable.executeGetByIds(ids)
        }
        movie
    }
}