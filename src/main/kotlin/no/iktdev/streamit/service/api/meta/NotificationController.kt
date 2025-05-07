package no.iktdev.streamit.service.api.meta

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.streamit.api.classes.fcm.FCMRemoteServerSetup
import no.iktdev.streamit.api.classes.fcm.FCMRemoteUserSetup
import no.iktdev.streamit.service.ApiRestController
import no.iktdev.streamit.service.services.RemoteDeviceNotificationService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@ApiRestController
@RequestMapping("/api/notification")
class NotificationController(
    open var service: RemoteDeviceNotificationService? = null
) {
    val log = KotlinLogging.logger {}



    @PostMapping("/push/configure/server")
    open fun sendConfigurationForServer(@RequestBody  data: FCMRemoteServerSetup) {
        val message = Message.builder()
            .putData("action", "no.iktdev.streamit.messaging.ConfigureServer")
            .putData("server", Gson().toJson(data.payload))

            .setToken(data.fcmReceiverId)
            .build()
        if (service == null || service?.firebaseApp == null) {
            log.error { "Service/FirebaseApp is null" }
        }
        service?.firebaseApp?.let { app ->
            FirebaseMessaging.getInstance(app).send(message)
            log.info { "Sending requested payload on 'configure-server' to FCM for ${data.fcmReceiverId}" }
        }
    }

}