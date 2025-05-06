package no.iktdev.streamit.shared.classes

data class RegisterDeviceData(
    val deviceId: String,
    val applicationPackageName: String,
    val osVersion: String,
    val osPlatform: String
)