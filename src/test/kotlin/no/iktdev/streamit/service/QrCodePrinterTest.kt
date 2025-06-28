package no.iktdev.streamit.service

import com.google.gson.Gson
import no.iktdev.streamit.service.services.ConfigValueService
import no.iktdev.streamit.shared.Env
import no.iktdev.streamit.shared.classes.fcm.clazzes.Server
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

class QrCodePrinterTest {



    @Test
    fun `should print QR code to console`() {
        mockFolders()

        val configValueService = ConfigValueService()

        val server = Server(
            id = configValueService.generateServerId(),
            name = "Streamit Server",
            lan = "http://192.168.1.1:8080",
            remote = "https://streamit.example.com",
            remoteSecure = true
        )
        configValueService.printQRCodeToConsole(Gson().toJson(server))
    }
}