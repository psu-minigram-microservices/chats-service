@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.routing

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import me.soknight.minigram.chats.dto.ChatDto
import me.soknight.minigram.chats.mockProfileClient
import me.soknight.minigram.chats.module
import me.soknight.minigram.chats.plugin.appJson
import me.soknight.minigram.chats.testToken
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.test.*

class ChatRoutesTest {
    private val userId = Uuid.random()

    private fun test(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        environment { config = ApplicationConfig("application.conf") }
        application { module(clientFactory = { mockProfileClient(userId) }) }
        block()
    }

    @Test fun `GET chats returns 200`() = test {
        val client = createClient { install(ContentNegotiation) { json(appJson) } }
        val response = client.get("/api/v1/chats") { bearerAuth(testToken(userId)) }
        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(response.body<List<ChatDto>>())
    }

    @Test fun `GET chats returns 401 without token`() = test {
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/chats").status)
    }

    @Test fun `POST chats creates SAVED chat and returns 201`() = test {
        val client = createClient { install(ContentNegotiation) { json(appJson) } }
        val response = client.post("/api/v1/chats") {
            bearerAuth(testToken(userId))
            contentType(ContentType.Application.Json)
            setBody("""{"type":"SAVED"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }
}
