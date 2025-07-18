package no.iktdev.streamit.service.api.user

import no.iktdev.streamit.library.db.tables.user.UserTable
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.shared.Mode
import no.iktdev.streamit.shared.RequiresAuthentication
import no.iktdev.streamit.shared.classes.User
import no.iktdev.streamit.shared.database.queries.executeDeleteWith
import no.iktdev.streamit.shared.database.queries.executeSelectAll
import no.iktdev.streamit.shared.database.queries.executeSelectWith
import no.iktdev.streamit.shared.database.queries.upsert
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@ApiRestController
@RequestMapping("/user")
class UserController {

    @RequiresAuthentication(Mode.Soft)
    @GetMapping(path = ["", "/all"])
    fun allUsers(): List<User> {
        return UserTable.executeSelectAll()
    }

    @RequiresAuthentication(Mode.Soft)
    @GetMapping("/{guid}")
    fun getUserByGuid(@PathVariable guid: String): User? {
        return UserTable.executeSelectWith(guid)
    }


    /**
     * Post Mapping below
     **/
    @RequiresAuthentication(Mode.Strict)
    @PostMapping()
    fun createOrUpdateUser(@RequestBody user: User): ResponseEntity<String> {
        UserTable.upsert(user)
        return ResponseEntity("User Updated or Created", HttpStatus.OK)
    }

    @RequiresAuthentication(Mode.Strict)
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