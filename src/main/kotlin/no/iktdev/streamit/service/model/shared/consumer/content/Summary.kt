package no.iktdev.streamit.service.model.shared.consumer.content

data class Summary(
    val id: Long,
    val description: String,
    val language: String,
    val catalogId: Long
)