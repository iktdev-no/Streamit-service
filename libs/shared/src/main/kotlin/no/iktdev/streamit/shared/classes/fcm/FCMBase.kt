package no.iktdev.streamit.api.classes.fcm

abstract class FCMBase(
    @Transient open val packageId: String,
    @Transient open val fcmSenderId: String,
    @Transient open val fcmReceiverId: String
)