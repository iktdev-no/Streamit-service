package no.iktdev.streamit.service.model.internal.user

import no.iktdev.streamit.service.model.shared.User as SharedUser

data class User(val id: Long, val uid: String, val name: String, val image: String) {
    fun asShared(): SharedUser = SharedUser(uid, name, image)
}

fun List<User>.asUserList(): List<SharedUser> =
    this.map { it.asShared() }