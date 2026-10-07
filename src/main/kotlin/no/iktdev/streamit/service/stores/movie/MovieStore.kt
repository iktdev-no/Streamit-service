package no.iktdev.streamit.service.stores.movie

import no.iktdev.streamit.service.db.tables.content.v2.MovieTableV2
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.model.shared.content.Movie
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class MovieStore: IMovieStore {

    override fun insert(
        catalogId: Long,
        videoId: Long
    ): Boolean = withTransaction {
        MovieTableV2.insert {
            it[MovieTableV2.catalogId] = catalogId
            it[MovieTableV2.videoId] = videoId
        }.insertedCount > 0
    }.isSuccess
}