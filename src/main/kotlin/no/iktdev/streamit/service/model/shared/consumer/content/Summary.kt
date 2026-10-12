package no.iktdev.streamit.service.model.shared.consumer.content

data class Summary(
    val id: Int,
    val description: String,
    val language: String,
    val catalogId: Int
)