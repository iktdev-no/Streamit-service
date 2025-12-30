package no.iktdev.streamit.service.dto

import java.io.Serializable

data class Server(
    val id: String,
    var name: String,
    val lan: String,
    val remote: String? = null,
    var remoteSecure: Boolean = false
) :
    Serializable