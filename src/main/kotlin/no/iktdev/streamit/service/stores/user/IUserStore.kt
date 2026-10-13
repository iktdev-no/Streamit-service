package no.iktdev.streamit.service.stores.user

import no.iktdev.streamit.service.model.internal.user.UserData
import no.iktdev.streamit.service.model.internal.user.User
import java.util.UUID

interface IUserStore {
    fun get(id: Long): User?
    fun getByUid(uid: UUID): User?
    fun getAll(): List<User>
    fun insert(user: UserData): User
    fun delete(uid: UUID): Boolean
    fun update(uid: UUID, name: String, image: String): User?
}