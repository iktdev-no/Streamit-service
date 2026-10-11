package no.iktdev.streamit.service.model.shared.consumer

import java.time.Instant

data class Progress(
    val played: Instant,
    val position: Long
)
