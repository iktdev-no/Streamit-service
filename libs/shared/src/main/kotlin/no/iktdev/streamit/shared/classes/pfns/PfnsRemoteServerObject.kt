package no.iktdev.streamit.shared.classes.pfns

import no.iktdev.streamit.shared.classes.fcm.clazzes.Server

data class PfnsRemoteServerObject(
    val serverId: String,
    val pfnsReceiverId: String, // FCM receiver ID for target device
    val server: Server
)