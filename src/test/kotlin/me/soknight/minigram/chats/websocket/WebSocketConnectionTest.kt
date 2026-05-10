@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.websocket

import io.ktor.client.plugins.websocket.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.withTimeoutOrNull
import me.soknight.minigram.chats.mockProfileClient
import me.soknight.minigram.chats.module
import me.soknight.minigram.chats.testToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class WebSocketConnectionTest {
    private val userId = Uuid.random()

    private fun test(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        environment { config = ApplicationConfig("application.conf") }
        application { module(clientFactory = { mockProfileClient(userId) }) }
        block()
    }

    @Test fun `connect without token closes with VIOLATED_POLICY`() = test {
        val client = createClient { install(WebSockets) }
        client.webSocket("/ws") {
            val reason = closeReason.await()
            assertEquals(CloseReason.Codes.VIOLATED_POLICY, reason?.knownReason)
        }
    }

    @Test fun `connect with invalid token closes with VIOLATED_POLICY`() = test {
        val client = createClient { install(WebSockets) }
        client.webSocket("/ws?token=not-a-valid-jwt") {
            val reason = closeReason.await()
            assertEquals(CloseReason.Codes.VIOLATED_POLICY, reason?.knownReason)
        }
    }

    @Test fun `connect with valid token stays open`() = test {
        val client = createClient { install(WebSockets) }
        client.webSocket("/ws?token=${testToken(userId)}") {
            val frame = withTimeoutOrNull(200.milliseconds) { incoming.receive() }
            assertNull(frame)
        }
    }
}
