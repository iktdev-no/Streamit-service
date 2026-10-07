package no.iktdev.streamit.service.controller.api.content

import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.model.shared.content.Progress
import no.iktdev.streamit.service.stores.progress.IProgressStore
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import java.time.Instant
import java.util.UUID

@ApiRestController
@RequestMapping(value = ["/v1/video/progress"])
class VideoProgressController(
    private val progressStore: IProgressStore
) {
    @RequiresAuthentication(Scope.ProgressRead)
    @GetMapping("/{userId}")
    fun getVideoProgress(@PathVariable userId: UUID): List<Progress> = progressStore.getAll(userId)

    @RequiresAuthentication(Scope.ProgressRead)
    @GetMapping("/{userId}/after/{played}")
    fun getProgressAfter(@PathVariable userId: UUID, @PathVariable played: Instant): List<Progress> =
        progressStore.getAfter(userId, played)

    @RequiresAuthentication(Scope.ProgressWrite)
    @PostMapping("/{userId}/video/{videoId}")
    fun updateProgress(@PathVariable userId: UUID, @PathVariable videoId: Long, @RequestBody progress: Progress): Boolean {
        return progressStore.upsert(userId, videoId, progress)
    }

    @RequiresAuthentication(Scope.ProgressRead)
    @GetMapping("/{userId}/video/{videoId}")
    fun getProgress(@PathVariable userId: UUID, @PathVariable videoId: Long): Progress? {
        return progressStore.get(userId, videoId)
    }
}
