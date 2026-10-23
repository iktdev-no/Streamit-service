package no.iktdev.streamit.service.model.shared.consumer

import java.io.Serializable

data class ServerSource(
    val id: String,
    val url: String,
    val kind: SourceKind,
    val priority: Int,
    val tls: SourceTls? = null
) : Serializable
