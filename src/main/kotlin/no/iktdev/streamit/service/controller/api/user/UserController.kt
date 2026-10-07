package no.iktdev.streamit.service.controller.api.user

import io.swagger.v3.oas.annotations.tags.Tag
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.model.internal.user.UserData
import no.iktdev.streamit.service.model.internal.user.asUserList
import no.iktdev.streamit.service.model.shared.User
import no.iktdev.streamit.service.stores.user.IUserStore
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@ApiRestController
@Tag(name = "Users", description = "User profiles and identity lookup")
@RequestMapping("/user")
class UserController(
    private val users: IUserStore
) {

    @RequiresAuthentication(Scope.UserRead)
    @GetMapping(path = ["", "/all"])
    fun allUsers(): List<User> {
        return users.getAll().asUserList()
    }

    @RequiresAuthentication(Scope.UserRead)
    @GetMapping("/{uid}")
    fun getUserByGuid(@PathVariable uid: UUID): User? {
        return users.getByUid(uid)?.asShared()
    }



    @RequiresAuthentication(Scope.UserWrite)
    @PostMapping
    fun createUser(@RequestBody user: UserData): User =
        users.insert(user).asShared()

    @RequiresAuthentication(Scope.UserWrite)
    @PatchMapping("/{uid}")
    fun updateUser(
        @PathVariable uid: UUID,
        @RequestBody user: UserData
    ): ResponseEntity<User> =
        users.update(uid, user.name, user.image)
            ?.let { ResponseEntity.ok(it.asShared()) }
            ?: ResponseEntity.notFound().build()

    @RequiresAuthentication(Scope.UserWrite)
    @DeleteMapping("/{uid}")
    fun deleteUser(@PathVariable uid: UUID): ResponseEntity<Void> =
        if (users.delete(uid)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
}
