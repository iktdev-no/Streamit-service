package no.iktdev.streamit.service.stores.continuewatch

import no.iktdev.streamit.service.model.shared.consumer.content.ContinueToWatch
import no.iktdev.streamit.service.model.shared.consumer.content.Progress
import java.util.UUID

interface IContinueWatchStore {
    fun getForUser(userId: UUID): List<ContinueToWatch>

    /**
     * Reconciles a progress update with the continue-watching pointer.
     * Must be called inside the transaction that persists the progress row.
     */
    fun syncFromProgress(userId: Long, videoId: Long, progress: Progress)
}