package no.iktdev.streamit.service.stores.genre

import no.iktdev.streamit.service.db.tables.content.v2.GenreTableV2
import no.iktdev.streamit.service.db.tables.util.withTransaction
import no.iktdev.streamit.service.model.shared.content.Genre
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class GenreStore : IGenreStore {

    override fun getById(id: Long): Genre? = withTransaction {
        GenreTableV2
            .selectAll()
            .where { GenreTableV2.id eq id }
            .singleOrNull()
            ?.toGenre()
    }.getOrNull()

    override fun getByName(name: String): Genre? = withTransaction {
        GenreTableV2
            .selectAll()
            .where { GenreTableV2.genre eq name }
            .singleOrNull()
            ?.toGenre()
    }.getOrNull()

    override fun getAll(): List<Genre> = withTransaction {
        GenreTableV2
            .selectAll()
            .orderBy(GenreTableV2.genre to SortOrder.ASC)
            .map { it.toGenre() }
    }.getOrElse { emptyList() }

    override fun getOrInsert(name: String): Genre = withTransaction {
        GenreTableV2
            .selectAll()
            .where { GenreTableV2.genre eq name }
            .singleOrNull()
            ?.toGenre()
            ?: GenreTableV2.insertAndGetId {
                it[genre] = name
            }.let { id ->
                Genre(
                    id = id.value.toInt(),
                    genre = name
                )
            }
    }.getOrThrow()

    private fun ResultRow.toGenre(): Genre =
        Genre(
            id = this[GenreTableV2.id].value.toInt(),
            genre = this[GenreTableV2.genre]
        )
}