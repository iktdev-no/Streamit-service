package no.iktdev.streamit.service.model.internal.pfns

import no.iktdev.streamit.service.dto.Server


data class PfnsRemoteServerObject(
    val serverId: String,
    val pfnsReceiverId: String, // FCM receiver ID for target device
    val server: Server
)