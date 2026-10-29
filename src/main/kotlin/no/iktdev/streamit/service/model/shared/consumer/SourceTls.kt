package no.iktdev.streamit.service.model.shared.consumer

import java.io.Serializable

enum class TlsMode {
    DefaultTrust,
    PinnedCertificate,
    AnyTrust
}

data class SourceTls(
    val mode: TlsMode,
    val pinnedCertificateSha256: String? = null
) : Serializable
