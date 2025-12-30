package no.iktdev.streamit.service

import no.iktdev.streamit.service.api.authentication.AuthenticationController
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import kotlin.test.Test

class RestControllerBeanLoadTest: TestBase() {

    @Autowired
    lateinit var context: ApplicationContext

    @Test
    fun `ValidateController should be loaded as a Spring bean`() {
        val bean = context.getBean(AuthenticationController::class.java)
        assertThat(bean).isNotNull
    }
}