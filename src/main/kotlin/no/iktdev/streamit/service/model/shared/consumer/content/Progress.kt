package no.iktdev.streamit.service.model.shared.consumer.content

import java.time.Instant

data class Progress(
    val played: Instant,
    val position: Long
)