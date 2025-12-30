package no.iktdev.streamit.service.dto.pfns

import no.iktdev.streamit.service.dto.Server

data class ServerConfigPushObject(
    val receiverId: String,
    val server: Server
)