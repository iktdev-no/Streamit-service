package no.iktdev.streamit.service.stores.progress

import no.iktdev.streamit.service.model.shared.content.Progress
import java.time.Instant
import java.util.UUID

interface IProgressStore {
    fun get(userId: UUID, videoId: Long): Progress?
    fun getAll(userId: UUID): List<Progress>
    fun getAfter(userId: UUID, played: Instant): List<Progress>
    fun upsert(userId: UUID, videoId: Long, progress: Progress): Boolean
}
