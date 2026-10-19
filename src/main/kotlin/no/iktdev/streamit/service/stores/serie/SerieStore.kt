package no.iktdev.streamit.service.stores.serie

import no.iktdev.streamit.service.db.tables.content.v2.SerieTableV2
import no.iktdev.streamit.service.db.tables.util.withTransaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class SerieStore: ISerieStore {

    override fun getVideoId(catalogId: Long, season: Int, episode: Int): Long? = withTransaction {
        SerieTableV2
            .selectAll()
            .where {
                (SerieTableV2.catalogId eq catalogId) and
                    (SerieTableV2.season eq season) and
                    (SerieTableV2.episode eq episode)
            }
            .singleOrNull()
            ?.get(SerieTableV2.videoId)
            ?.value
    }.getOrNull()

    override fun insert(
        catalogId: Long,
        videoId: Long,
        season: Int,
        episode: Int,
        title: String?
    ): Boolean = withTransaction {
        SerieTableV2.insertIgnore {
            it[SerieTableV2.catalogId] = catalogId
            it[SerieTableV2.videoId] = videoId
            it[SerieTableV2.season] = season
            it[SerieTableV2.episode] = episode
            it[SerieTableV2.title] = title
        }.insertedCount > 0
    }.isSuccess
}
