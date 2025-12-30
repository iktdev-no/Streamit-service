package no.iktdev.streamit.service.dto

data class RegisterDeviceData(
    val deviceId: String,
    val applicationPackageName: String,
    val osVersion: String,
    val osPlatform: String
)