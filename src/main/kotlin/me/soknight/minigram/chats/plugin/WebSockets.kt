package me.soknight.minigram.chats.plugin

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.hide
import io.ktor.server.websocket.*
import io.ktor.utils.io.ExperimentalKtorApi
import io.ktor.websocket.*
import me.soknight.minigram.chats.auth.JwtTokenProvider
import me.soknight.minigram.chats.client.ProfileClientFactory
import me.soknight.minigram.chats.websocket.WebSocketConnectionManager
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalKtorApi::class)
fun Application.configureWebSockets(
    jwtTokenProvider: JwtTokenProvider,
    connectionManager: WebSocketConnectionManager,
    profileClientFactory: ProfileClientFactory
) {
    install(WebSockets) {
        pingPeriod = 30.seconds
        timeout    = 60.seconds
    }
    routing {
        route("/ws") {
            webSocket {
                val token = call.request.queryParameters["token"]
                    ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "token required"))
                jwtTokenProvider.validateAndGetUserId(token)
                    ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "invalid token"))
                val profileId = profileClientFactory.create(token).resolveMyProfileId()

                connectionManager.register(profileId, this)
                try {
                    for (frame in incoming) { /* client-to-server frames ignored */ }
                } finally {
                    connectionManager.unregister(profileId, this)
                }
            }
        }.hide()
    }
}
