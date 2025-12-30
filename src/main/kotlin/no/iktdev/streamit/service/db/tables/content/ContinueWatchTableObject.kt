package no.iktdev.streamit.service.db.tables.content

data class ContinueWatchTableObject(
    val userId: String,
    val type: String,
    val title: String,
    val collection: String?,
    val hide: Boolean
)