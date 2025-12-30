package no.iktdev.streamit.service.api.user

import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.db.tables.user.UserTable
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.dto.User
import no.iktdev.streamit.service.db.queries.executeDeleteWith
import no.iktdev.streamit.service.db.queries.executeSelectAll
import no.iktdev.streamit.service.db.queries.executeSelectWith
import no.iktdev.streamit.service.db.queries.upsert
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@ApiRestController
@RequestMapping("/user")
class UserController {

    @RequiresAuthentication(Scope.UserRead)
    @GetMapping(path = ["", "/all"])
    fun allUsers(): List<User> {
        return UserTable.executeSelectAll()
    }

    @RequiresAuthentication(Scope.UserRead)
    @GetMapping("/{guid}")
    fun getUserByGuid(@PathVariable guid: String): User? {
        return UserTable.executeSelectWith(guid)
    }


    /**
     * Post Mapping below
     **/
    @RequiresAuthentication(Scope.UserWrite)
    @PostMapping()
    fun createOrUpdateUser(@RequestBody user: User): ResponseEntity<String> {
        UserTable.upsert(user)
        return ResponseEntity("User Updated or Created", HttpStatus.OK)
    }

    @RequiresAuthentication(Scope.UserWrite)
    @DeleteMapping("")
    fun deleteUser(@RequestBody userId: String): ResponseEntity<String>
    {
        val succeeded = UserTable.executeDeleteWith(userId)
        return if (succeeded)
            ResponseEntity("Deleted user ${userId} with Guid ${userId}", HttpStatus.OK)
        else
            ResponseEntity("Could not find user ${userId} with Guid ${userId} to be deleted", HttpStatus.NOT_FOUND)
    }


}