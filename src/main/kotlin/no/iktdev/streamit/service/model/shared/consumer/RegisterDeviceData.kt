package no.iktdev.streamit.service.model.shared.consumer

data class RegisterDeviceData(
    val deviceId: String,
    val applicationPackageName: String,
    val osVersion: String,
    val osPlatform: String
)