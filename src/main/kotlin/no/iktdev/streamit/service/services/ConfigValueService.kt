package no.iktdev.streamit.service.services

import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import mu.KotlinLogging
import no.iktdev.streamit.service.Env
import no.iktdev.streamit.service.model.shared.consumer.Server
import no.iktdev.streamit.service.model.shared.consumer.ServerSource
import no.iktdev.streamit.service.model.shared.consumer.SourceKind
import no.iktdev.streamit.service.model.shared.consumer.SourceTls
import no.iktdev.streamit.service.model.shared.consumer.TlsMode
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.util.UUID

@Service
class ConfigValueService {
    val log = KotlinLogging.logger {}
    lateinit var serverId: String
    var server: Server? = null

    init {
        loadConfiguration()
        setServerObject()
    }

    fun generateServerId(): String {
        return UUID.randomUUID().toString().uppercase().substringAfterLast("-")
    }


    fun loadConfiguration() {
        val serverIdFile = Env.getConfigFolder().resolve("id")

        if (!Env.getConfigFolder().exists()) {
            Env.getConfigFolder().mkdirs()
        }

        if (!serverIdFile.exists()) {
            val serverId = generateServerId().also { serverId = it }
            if (!serverIdFile.exists()) {
                serverIdFile.createNewFile()
            }
            serverIdFile.writeText(serverId)
        } else {
            serverId = serverIdFile.readText()
        }


        val avahiServiceFolder = if (Env.getAvahiServiceFolder().exists()) Env.getAvahiServiceFolder() else Env.getConfigFolder().resolve("avahi")
        if (avahiServiceFolder.exists()) {
            val avahiContent = generateAvahi()
            val avahiFile = avahiServiceFolder.resolve("streamit_${serverId}.service")
            if (!avahiFile.exists()) {
                avahiFile.createNewFile()
            }
            avahiFile.writeText(avahiContent)
        } else {
            log.warn { "Avahi service folder does not exist, skipping Avahi service generation." }
        }
    }

    fun setServerObject() {
        if (Env.lanAddress.isNullOrBlank()) {
            log.error { "LAN address is blank, cannot create Server object" }
            return
        }

        val sources = buildList {
            add(
                ServerSource(
                    id = "lan",
                    url = normalizeSourceUrl(Env.lanAddress!!, defaultScheme = "http"),
                    kind = SourceKind.Lan,
                    priority = 0
                )
            )
            Env.wanAddress?.takeIf(String::isNotBlank)?.let { address ->
                val url = normalizeSourceUrl(address, defaultScheme = "https")
                val tls = if (url.startsWith("https://", ignoreCase = true)) {
                    if (Env.isSelfSignedUsed) {
                        SourceTls(TlsMode.PinnedCertificate, Env.wanCertificateSha256)
                    } else {
                        SourceTls(TlsMode.DefaultTrust)
                    }
                } else {
                    null
                }
                add(
                    ServerSource(
                        id = "remote",
                        url = url,
                        kind = SourceKind.Remote,
                        priority = 100,
                        tls = tls
                    )
                )
            }
        }

        server = Server(
            id = serverId,
            name = "Streamit Server",
            sources = sources
        )
        printQRCodeToConsole(Gson().toJson(server))
    }

    private fun normalizeSourceUrl(address: String, defaultScheme: String): String {
        val trimmed = address.trim().trimEnd('/')
        return if (trimmed.matches(Regex("^[A-Za-z][A-Za-z0-9+.-]*://.*"))) {
            trimmed
        } else {
            "$defaultScheme://$trimmed"
        }
    }

    fun printQRCodeToConsole(content: String) {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 20, 20)

        for (y in 0 until bitMatrix.height step 2) {
            for (x in 0 until bitMatrix.width) {
                val upper = bitMatrix[x, y]
                val lower = if (y + 1 < bitMatrix.height) bitMatrix[x, y + 1] else false
                print(when {
                    upper && lower -> "█"
                    upper -> "▀"
                    lower -> "▄"
                    else -> " "
                })
            }
            println()
        }
    }

    fun generateQRCode(width: Int = 200, height: Int = 200): BufferedImage {
        val content = Gson().toJson(server)
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, mapOf(EncodeHintType.MARGIN to 0))

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                image.setRGB(x, y, if (bitMatrix[x, y]) 0x000000 else 0xFFFFFF)
            }
        }
        return image
    }

    fun generateAvahi(): String = """
        <?xml version="1.0" standalone='no'?>
        <!DOCTYPE service-group SYSTEM "avahi-service.dtd">
        <service-group>
            <name replace-wildcards="yes">${Env.avahiServiceName}</name>
            <service>
                <type>_streamit._tcp</type>
                <txt-record>id=${serverId}</txt-record>
            </service>
        </service-group>
    """.trimIndent()


}
