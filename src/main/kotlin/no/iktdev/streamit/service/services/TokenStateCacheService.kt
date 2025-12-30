package no.iktdev.streamit.service.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterWrite
import no.iktdev.streamit.service.db.tables.pfns.TokenTable
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.minutes

enum class TokenState {
    Revoked,
    Active,
    NotFound
}

@Service
class TokenStateCacheService {

    private val cachedToken: Cache<String, TokenState> = Caffeine.newBuilder()
        .expireAfterWrite(15.minutes)
        .maximumSize(10_000)
        .build()

    fun getTokenState(token: String): TokenState {
        return cachedToken.get(token) {
            checkRevokedByToken(token)
        } ?: TokenState.NotFound
    }



    fun revokeToken(token: String) {
        cachedToken.put(token, TokenState.Revoked)
    }


    private fun checkRevokedByToken(token: String): TokenState {
        return TokenTable.findStateByToken(token)?.let {
            if (it.revoked) TokenState.Revoked else TokenState.Active
        } ?: TokenState.NotFound
    }

}