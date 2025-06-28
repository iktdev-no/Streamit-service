package no.iktdev.streamit.service.services

import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import mu.KotlinLogging
import no.iktdev.exfl.using
import no.iktdev.streamit.shared.Env
import no.iktdev.streamit.shared.classes.fcm.clazzes.Server
import org.springframework.stereotype.Service
import org.w3c.dom.Document
import java.awt.image.BufferedImage
import java.security.MessageDigest
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

@Service
class ConfigValueService {
    val log = KotlinLogging.logger {}
    lateinit var serverId: String
    lateinit var fingerprint: String
    var server: Server? = null

    init {
        loadConfiguration()
        setServerObject()
    }

    private fun generateServerId(): String {
        return UUID.randomUUID().toString().uppercase().substringAfterLast("-")
    }

    private fun generateFingerprint(): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(UUID.randomUUID().toString().toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun loadConfiguration() {
        val serverIdFile = Env.configFilesFolder.using("id")
        val serverFingerprintFile = Env.configFilesFolder.using("fingerprint")

        if (!Env.configFilesFolder.exists()) {
            Env.configFilesFolder.mkdirs()
        }

        if (!serverIdFile.exists()) {
            val serverId = generateServerId().also { serverId = it }
            serverIdFile.writeText(serverId)
        } else {
            serverId = serverIdFile.readText()
        }

        if (!serverFingerprintFile.exists()) {
            val fingerprint = generateFingerprint().also { this.fingerprint = it }
            serverFingerprintFile.writeText(fingerprint)
        } else {
            fingerprint = serverFingerprintFile.readText()
        }

        val avahiServiceFolder = if (Env.avahiServiceFolder.exists()) Env.avahiServiceFolder else Env.configFilesFolder.using("avahi")
        if (avahiServiceFolder.exists()) {
            val avahiContent = generateAvahi()
            val avahiFile = avahiServiceFolder.using("streamit_${serverId}.service")
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

        server = Server(
            id = serverId,
            name = "Streamit Server",
            fingerprint = fingerprint,
            lan = Env.lanAddress!!,
            remote = Env.wanAddress,
            remoteSecure = !Env.isSelfSignedUsed
        )
        printQRCodeToConsole(Gson().toJson(server))
    }

    fun printQRCodeToConsole(content: String) {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 20, 20)

        for (y in 0 until bitMatrix.height) {
            for (x in 0 until bitMatrix.width) {
                print(if (bitMatrix[x, y]) "██" else "  ")
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

    fun generateAvahi(): String {
        val lan = Env.lanAddress?.let {
            if (it.startsWith("http")) {
                it
            } else {
                "http://$it"
            }
        }

        val wan = Env.wanAddress?.let {
            if (it.startsWith("https")) {
                it
            } else {
                "https://$it"
            }
        }

        return """
            <?xml version="1.0" standalone='no'?>
            <!DOCTYPE service-group SYSTEM "avahi-service.dtd">
            <service-group>
                <name replace-wildcards="yes">%h Streamit</name>
                <service>
                    <type>_streamit._tcp</type>
                    <txt-record>id=${serverId}</txt-record>
                    <txt-record>fingerprint=${fingerprint}</txt-record>
                    <txt-record>lan=${lan}</txt-record>
                    <txt-record>remote=${wan}</txt-record>
                    <txt-record>remoteSecure=${!Env.isSelfSignedUsed}</txt-record>
                </service>
            </service-group>
        """.trimIndent()
    }


}