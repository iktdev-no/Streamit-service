package no.iktdev.streamit.service.model.shared.consumer

data class ImportReference(
    val catalogId: Long? = null,
    val store: String // Also known as collection
)