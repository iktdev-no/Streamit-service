package no.iktdev.streamit.service.model.shared.consumer

data class Title(
    val id: Int,
    val title: String,
    val language: String,
    val preferred: Boolean = false,
)

data class TitleMatch(
    val catalogId: Long,
    val title: String,
)