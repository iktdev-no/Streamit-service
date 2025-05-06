package no.iktdev.streamit.api.classes.fcm

import no.iktdev.streamit.shared.classes.fcm.clazzes.Server

data class FCMRemoteServerSetup(
    override val packageId: String,
    override val fcmSenderId: String,
    override val fcmReceiverId: String,
    val payload: Server
) : FCMBase(packageId = packageId, fcmSenderId = fcmSenderId, fcmReceiverId = fcmReceiverId)