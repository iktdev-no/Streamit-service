package no.iktdev.streamit.service.model.shared.consumer

data class Genre(val id: Int, val genre: String)
{
}

data class GenreCatalog(
    val genre: Genre,
    val catalog: List<Catalog>,
    val offset: Int,
    val hasMore: Boolean
)