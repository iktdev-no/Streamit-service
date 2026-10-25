package no.iktdev.streamit.service.model.shared.consumer.content

data class Subtitle(
    val id: Long,
    val language: String,
    val file: String,
    val format: String
) {
}