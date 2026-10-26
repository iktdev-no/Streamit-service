package no.iktdev.streamit.service.stores.subtitle

import no.iktdev.streamit.service.db.tables.content.v2.SubtitleTableV2
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.model.shared.consumer.content.Subtitle
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component

@Component
class SubtitleStore : ISubtitleStore {

    override fun insert(
        videoId: Long,
        language: String,
        format: String,
        file: String
    ): Subtitle =
        withTransaction {
            SubtitleTableV2.insertIgnore {
                it[SubtitleTableV2.videoId] = videoId
                it[SubtitleTableV2.language] = language
                it[SubtitleTableV2.format] = format
                it[SubtitleTableV2.file] = file

            }

            SubtitleTableV2
                .selectAll()
                .where {
                    (SubtitleTableV2.videoId eq videoId) and
                        (SubtitleTableV2.language eq language) and
                        (SubtitleTableV2.format eq format)
                }
                .single()
                .toSubtitle()
        }.getOrThrow()

    override fun update(
        id: Long,
        language: String,
        format: String,
        file: String
    ): Subtitle? =
        withTransaction {
            val updated = SubtitleTableV2.update(
                where = {
                    SubtitleTableV2.id eq id
                }
            ) {
                it[SubtitleTableV2.language] = language
                it[SubtitleTableV2.format] = format
                it[SubtitleTableV2.file] = file
            }

            if (updated == 0) {
                return@withTransaction null
            }

            SubtitleTableV2
                .selectAll()
                .where {
                    SubtitleTableV2.id eq id
                }
                .single()
                .toSubtitle()
        }.getOrThrow()

    override fun delete(id: Long) {
        withTransaction {
            SubtitleTableV2.deleteWhere {
                SubtitleTableV2.id eq id
            }
        }.getOrThrow()
    }

    private fun ResultRow.toSubtitle(): Subtitle =
        Subtitle(
            id = this[SubtitleTableV2.id].value,
            language = this[SubtitleTableV2.language],
            format = this[SubtitleTableV2.format],
            fileName = this[SubtitleTableV2.file]
        )
}
