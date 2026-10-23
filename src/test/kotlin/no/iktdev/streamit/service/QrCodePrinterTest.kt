package no.iktdev.streamit.service

import com.google.gson.Gson
import no.iktdev.streamit.service.services.ConfigValueService
import no.iktdev.streamit.service.model.shared.consumer.Server
import no.iktdev.streamit.service.model.shared.consumer.ServerSource
import no.iktdev.streamit.service.model.shared.consumer.SourceKind
import org.junit.jupiter.api.Test

class QrCodePrinterTest {



    @Test
    fun `should print QR code to console`() {
        mockFolders()

        val configValueService = ConfigValueService()

        val server = Server(
            id = configValueService.generateServerId(),
            name = "Streamit Server",
            sources = listOf(
                ServerSource(
                    id = "lan",
                    url = "http://192.168.1.1:8080",
                    kind = SourceKind.Lan,
                    priority = 0
                )
            )
        )
        configValueService.printQRCodeToConsole(Gson().toJson(server))
    }
}