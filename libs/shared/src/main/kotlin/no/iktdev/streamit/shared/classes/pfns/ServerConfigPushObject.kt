package no.iktdev.streamit.shared.classes.pfns

import no.iktdev.streamit.shared.classes.fcm.clazzes.Server

data class ServerConfigPushObject(
    val receiverId: String,
    val server: Server
)