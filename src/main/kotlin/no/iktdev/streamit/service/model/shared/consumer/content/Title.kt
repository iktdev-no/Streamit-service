package no.iktdev.streamit.service.model.shared.consumer.content

data class Title(
    val id: Long,
    val title: String,
    val language: String,
    val preferred: Boolean = false,
)

data class TitleMatch(
    val catalogId: Long,
    val title: String,
)