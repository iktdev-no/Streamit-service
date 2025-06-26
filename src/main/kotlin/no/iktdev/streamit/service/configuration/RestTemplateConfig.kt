package no.iktdev.streamit.service.configuration

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateConfig {

    @Bean
    fun pfnsRestTemplate(): RestTemplate {
        return RestTemplateBuilder()
            .rootUri("https://pfns.iktdev.no")
            .build()
    }
}