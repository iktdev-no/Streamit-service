package no.iktdev.streamit.service.stores.summary

import no.iktdev.streamit.service.db.tables.content.v2.SummaryTableV2
import no.iktdev.streamit.service.model.shared.content.Summary
import no.iktdev.streamit.service.db.tables.util.withTransaction
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component

@Component
class SummaryStore : ISummaryStore {

    override fun get(summaryId: Long): Summary? =
        withTransaction {
            SummaryTableV2
                .selectAll()
                .where {
                    SummaryTableV2.id eq summaryId
                }
                .singleOrNull()
                ?.toSummary()
        }.getOrNull()

    override fun getAll(catalogId: Long): List<Summary> =
        withTransaction {
            SummaryTableV2
                .selectAll()
                .where {
                    SummaryTableV2.catalogId eq catalogId
                }
                .orderBy(
                    SummaryTableV2.language to SortOrder.ASC
                )
                .map { it.toSummary() }
        }.getOrDefault(emptyList())

    override fun insert(
        catalogId: Long,
        description: String,
        language: String
    ): Summary =
        withTransaction {
            val id = SummaryTableV2.insert {
                it[SummaryTableV2.catalogId] = catalogId
                it[SummaryTableV2.description] = description
                it[SummaryTableV2.language] = language
            } get SummaryTableV2.id

            Summary(
                id = id.value.toInt(),
                description = description,
                language = language,
                catalogId = catalogId.toInt()
            )
        }.getOrThrow()

    override fun update(
        id: Long,
        description: String,
        language: String
    ): Summary? =
        withTransaction {
            val updated = SummaryTableV2.update(
                where = {
                    SummaryTableV2.id eq id
                }
            ) {
                it[SummaryTableV2.description] = description
                it[SummaryTableV2.language] = language
            }

            if (updated == 0) {
                return@withTransaction null
            }

            SummaryTableV2
                .selectAll()
                .where {
                    SummaryTableV2.id eq id
                }
                .single()
                .toSummary()
        }.getOrNull()

    override fun delete(id: Long) {
        withTransaction {
            SummaryTableV2.deleteWhere {
                SummaryTableV2.id eq id
            }
        }
    }

    private fun ResultRow.toSummary(): Summary =
        Summary(
            id = this[SummaryTableV2.id].value.toInt(),
            description = this[SummaryTableV2.description],
            language = this[SummaryTableV2.language],
            catalogId = this[SummaryTableV2.catalogId].value.toInt()
        )
}