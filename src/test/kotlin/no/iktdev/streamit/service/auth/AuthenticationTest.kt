package no.iktdev.streamit.service.auth

import com.auth0.jwt.interfaces.DecodedJWT
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class AuthenticationTest {
    private lateinit var auth: Authentication

    @BeforeEach
    fun setUp() {
        auth = spyk(Authentication())
    }

    @Test
    fun `should return false when token is null`() {
        val result = auth.isTokenValid(null)
        assertFalse(result)
    }

    @Test
    fun `should return false when token is empty`() {
        val result = auth.isTokenValid("")
        assertFalse(result)
    }

    @Test
    fun `should return false when decode returns null`() {
        every { auth.decode("invalidToken") } returns null

        val result = auth.isTokenValid("invalidToken")
        assertFalse(result)
    }

    @Test
    fun `should return false when token is expired`() {
        val decoded = mockk<DecodedJWT>()
        every { decoded.expiresAtAsInstant } returns Instant.now().minusSeconds(60)
        every { auth.decode("expiredToken") } returns decoded

        val result = auth.isTokenValid("expiredToken")
        assertFalse(result)
    }

    @Test
    fun `should return true when token is still valid`() {
        val decoded = mockk<DecodedJWT>()
        every { decoded.expiresAtAsInstant } returns Instant.now().plusSeconds(3600)
        every { auth.decode("validToken") } returns decoded

        val result = auth.isTokenValid("validToken")
        assertTrue(result)
    }

    @Test
    fun `should return true when token has no expiry`() {
        val decoded = mockk<DecodedJWT>()
        every { decoded.expiresAtAsInstant } returns null
        every { auth.decode("noExpiryToken") } returns decoded

        val result = auth.isTokenValid("noExpiryToken")
        assertTrue(result)
    }
}