package no.iktdev.streamit.service.model.shared.consumer

import java.io.Serializable

data class Server(
    val id: String,
    val name: String,
    val sources: List<ServerSource>
) : Serializable
