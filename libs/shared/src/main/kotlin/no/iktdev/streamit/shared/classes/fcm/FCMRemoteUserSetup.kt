package no.iktdev.streamit.api.classes.fcm

import no.iktdev.streamit.shared.classes.User

data class FCMRemoteUserSetup(
    override val packageId: String,
    override val fcmSenderId: String,
    override val fcmReceiverId: String,
    val payload: User
) : FCMBase(packageId = packageId, fcmSenderId = fcmSenderId, fcmReceiverId = fcmReceiverId)