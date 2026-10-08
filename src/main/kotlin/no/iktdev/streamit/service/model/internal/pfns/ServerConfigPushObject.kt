package no.iktdev.streamit.service.model.internal.pfns

import no.iktdev.streamit.service.model.shared.Server

data class ServerConfigPushObject(
    val receiverId: String,
    val server: Server
)