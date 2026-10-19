package no.iktdev.streamit.service.stores.movie

import no.iktdev.streamit.service.db.tables.content.v2.MovieTableV2
import no.iktdev.streamit.service.db.tables.util.withTransaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class MovieStore: IMovieStore {

    override fun insert(
        catalogId: Long,
        videoId: Long
    ): Boolean = withTransaction {
        val alreadyLinked = MovieTableV2
            .selectAll()
            .where {
                (MovieTableV2.catalogId eq catalogId) and
                    (MovieTableV2.videoId eq videoId)
            }
            .any()

        if (alreadyLinked) {
            return@withTransaction true
        }

        MovieTableV2.insert {
            it[MovieTableV2.catalogId] = catalogId
            it[MovieTableV2.videoId] = videoId
        }.insertedCount > 0
    }.isSuccess
}
