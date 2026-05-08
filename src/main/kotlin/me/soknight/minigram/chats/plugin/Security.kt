@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.plugin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import me.soknight.minigram.chats.config.JwtConfig
import me.soknight.minigram.chats.dto.ErrorDto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class UserPrincipal(val userId: Uuid) : Principal

fun Application.configureSecurity(config: JwtConfig) {
    install(Authentication) {
        jwt("jwt") {
            realm = "chats-service"
            verifier(
                JWT.require(Algorithm.HMAC256(config.secret))
                    .withIssuer(config.issuer)
                    .withAudience(config.audience)
                    .build()
            )
            validate { credential ->
                credential.payload.subject
                    ?.let { runCatching { Uuid.parse(it) }.getOrNull() }
                    ?.let { UserPrincipal(it) }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorDto("unauthorized", "Authentication required"))
            }
        }
    }
}

fun ApplicationCall.currentUserId(): Uuid =
    principal<UserPrincipal>()?.userId ?: error("Unauthenticated call outside auth block")
