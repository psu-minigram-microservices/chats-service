@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import me.soknight.minigram.chats.config.JwtConfig
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.koin.core.annotation.Single

@Single
class JwtTokenProvider(config: JwtConfig) {
    private val verifier = JWT.require(Algorithm.HMAC256(config.secret))
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    fun validateAndGetUserId(token: String): Uuid? =
        runCatching { Uuid.parse(verifier.verify(token).subject) }.getOrNull()
}
