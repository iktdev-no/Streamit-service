package no.iktdev.streamit.service.stores.title

import no.iktdev.streamit.service.db.tables.content.v2.CatalogTableV2
import no.iktdev.streamit.service.db.tables.content.v2.CatalogTitleTableV2
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.model.shared.CollectionReference
import no.iktdev.streamit.service.model.shared.content.Title
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component
import java.text.Normalizer

@Component
class TitleStore : ITitleStore {

    override fun get(id: Long): Title? = withTransaction {
        CatalogTitleTableV2
            .selectAll()
            .where { CatalogTitleTableV2.id eq id }
            .singleOrNull()
            ?.toTitle()
    }.getOrNull()

    override fun insert(
        catalogId: Long,
        title: String,
        language: String,
        preferred: Boolean
    ): Title = withTransaction {
        val id = CatalogTitleTableV2.insertAndGetId {
            it[CatalogTitleTableV2.catalogId] = EntityID(catalogId, CatalogTableV2)
            it[CatalogTitleTableV2.title] = title
            it[CatalogTitleTableV2.language] = language
            it[CatalogTitleTableV2.preferred] = preferred
        }

        Title(
            id = id.value.toInt(),
            title = title,
            language = language,
            preferred = preferred
        )
    }.getOrThrow()

    override fun update(
        id: Long,
        title: String,
        language: String
    ): Title? = withTransaction {
        val updated = CatalogTitleTableV2.update(
            where = { CatalogTitleTableV2.id eq id }
        ) {
            it[CatalogTitleTableV2.title] = title
            it[CatalogTitleTableV2.language] = language
        }

        if (updated == 0) {
            return@withTransaction null
        }

        CatalogTitleTableV2
            .selectAll()
            .where { CatalogTitleTableV2.id eq id }
            .single()
            .toTitle()
    }.getOrNull()

    override fun setPreferred(id: Long, preferred: Boolean) {
        withTransaction {
            CatalogTitleTableV2.update(
                where = { CatalogTitleTableV2.id eq id }
            ) {
                it[CatalogTitleTableV2.preferred] = preferred
            }
        }
    }

    override fun getAll(catalogId: Long): List<Title> = withTransaction {
        CatalogTitleTableV2
            .selectAll()
            .where {
                CatalogTitleTableV2.catalogId eq catalogId
            }
            .orderBy(
                CatalogTitleTableV2.preferred to SortOrder.DESC,
                CatalogTitleTableV2.language to SortOrder.ASC
            )
            .map { it.toTitle() }
    }.getOrElse { emptyList() }

    private fun ResultRow.toTitle(): Title =
        Title(
            id = this[CatalogTitleTableV2.id].value.toInt(),
            title = this[CatalogTitleTableV2.title],
            language = this[CatalogTitleTableV2.language],
            preferred = this[CatalogTitleTableV2.preferred]
        )

    override fun findCollectionByTitles(
        titles: List<String>
    ): CollectionReference? = withTransaction {

        val normalizedTitles = titles
            .filter { it.isNotBlank() }
            .map { it.normalize() }
            .toSet()

        if (normalizedTitles.isEmpty()) {
            return@withTransaction null
        }

        val catalogIds = CatalogTitleTableV2
            .selectAll()
            .filter {
                it[CatalogTitleTableV2.title].normalize() in normalizedTitles
            }
            .map {
                it[CatalogTitleTableV2.catalogId].value.toLong()
            }
            .distinct()

        if (catalogIds.size != 1) {
            return@withTransaction null
        }

        val catalogId = catalogIds.single()

        CatalogTableV2
            .selectAll()
            .where { CatalogTableV2.id eq catalogId }
            .singleOrNull()
            ?.let {
                CollectionReference(
                    catalogId = catalogId,
                    store = it[CatalogTableV2.store]
                )
            }
    }.getOrNull()

    private fun String.normalize(): String {
        val normalized = Normalizer.normalize(this, Normalizer.Form.NFKD)
        return normalized
            .replace("\\p{M}".toRegex(), "")
            .lowercase()
    }
}