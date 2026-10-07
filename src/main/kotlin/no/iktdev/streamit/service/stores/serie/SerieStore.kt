package no.iktdev.streamit.service.stores.serie

import no.iktdev.streamit.service.db.tables.content.v2.SerieTableV2
import no.iktdev.streamit.service.db.tables.util.withTransaction
import org.jetbrains.exposed.sql.insert
import org.springframework.stereotype.Component

@Component
class SerieStore: ISerieStore {

    override fun insert(
        catalogId: Long,
        videoId: Long,
        season: Int,
        episode: Int,
        title: String?
    ): Boolean = withTransaction {
        SerieTableV2.insert {
            it[SerieTableV2.catalogId] = catalogId
            it[SerieTableV2.videoId] = videoId
            it[SerieTableV2.season] = season
            it[SerieTableV2.episode] = episode
            it[SerieTableV2.title] = title
        }.insertedCount > 0
    }.isSuccess
}