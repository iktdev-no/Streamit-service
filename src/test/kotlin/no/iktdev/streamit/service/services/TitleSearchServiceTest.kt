package no.iktdev.streamit.service.services

import no.iktdev.streamit.service.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TitleSearchServiceTest: TestBase() {
    @Autowired lateinit var titleSearchService: TitleSearchService

    @Test
    fun verifyThatNonLatinGetsSanitizedProperly() {
        val text = "Айрис"
        val sanitized = titleSearchService.sanitizeTitle(text)
        assertThat(sanitized).isBlank
        assertThat(titleSearchService.findMasterBySanitized(sanitized)).isNull()
    }

}