@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.routing

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import me.soknight.minigram.chats.dto.KeyBackupDto
import me.soknight.minigram.chats.dto.UserPublicKeyDto
import me.soknight.minigram.chats.mockProfileClientFactory
import me.soknight.minigram.chats.plugin.appJson
import me.soknight.minigram.chats.setup
import me.soknight.minigram.chats.testToken
import org.koin.dsl.module
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.test.*

class UserKeyRoutesTest {
    private val userId = Uuid.random()

    private fun test(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        environment { config = ApplicationConfig("application.conf") }
        application { setup(module { single { mockProfileClientFactory(userId) } }) }
        block()
    }

    @Test fun `PUT keys returns 401 without token`() = test {
        val res = client.put("/api/v1/keys") {
            contentType(ContentType.Application.Json)
            setBody("""{"public_key":"abc"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test fun `PUT keys stores public key and GET keys returns it`() = test {
        val client = createClient { install(ContentNegotiation) { json(appJson) } }
        val pubKey = "dGVzdC1wdWJsaWMta2V5"
        client.put("/api/v1/keys") {
            bearerAuth(testToken(userId))
            contentType(ContentType.Application.Json)
            setBody("""{"public_key":"$pubKey"}""")
        }.let { assertEquals(HttpStatusCode.OK, it.status) }

        val res = client.get("/api/v1/keys/$userId") { bearerAuth(testToken(userId)) }
        assertEquals(HttpStatusCode.OK, res.status)
        val dto = res.body<UserPublicKeyDto>()
        assertEquals(pubKey, dto.publicKey)
    }

    @Test fun `GET keys unknown user returns 404`() = test {
        val client = createClient { install(ContentNegotiation) { json(appJson) } }
        val res = client.get("/api/v1/keys/${Uuid.random()}") { bearerAuth(testToken(userId)) }
        assertEquals(HttpStatusCode.NotFound, res.status)
    }

    @Test fun `PUT keys backup without prior public key returns 404`() = test {
        val client = createClient { install(ContentNegotiation) { json(appJson) } }
        val res = client.put("/api/v1/keys/backup") {
            bearerAuth(testToken(userId))
            contentType(ContentType.Application.Json)
            setBody("""{"salt":"s","iv":"i","ciphertext":"c"}""")
        }
        assertEquals(HttpStatusCode.NotFound, res.status)
    }

    @Test fun `PUT backup then GET backup round-trip`() = test {
        val client = createClient { install(ContentNegotiation) { json(appJson) } }
        // Set public key first
        client.put("/api/v1/keys") {
            bearerAuth(testToken(userId))
            contentType(ContentType.Application.Json)
            setBody("""{"public_key":"key123"}""")
        }
        // Store backup
        client.put("/api/v1/keys/backup") {
            bearerAuth(testToken(userId))
            contentType(ContentType.Application.Json)
            setBody("""{"salt":"mysalt","iv":"myiv","ciphertext":"myct"}""")
        }.let { assertEquals(HttpStatusCode.OK, it.status) }

        // Retrieve backup
        val res = client.get("/api/v1/keys/backup") { bearerAuth(testToken(userId)) }
        assertEquals(HttpStatusCode.OK, res.status)
        val dto = res.body<KeyBackupDto>()
        assertEquals("mysalt", dto.salt)
        assertEquals("myiv", dto.iv)
        assertEquals("myct", dto.ciphertext)
    }

    @Test fun `GET backup without backup stored returns 404`() = test {
        val client = createClient { install(ContentNegotiation) { json(appJson) } }
        // Set public key without backup
        client.put("/api/v1/keys") {
            bearerAuth(testToken(userId))
            contentType(ContentType.Application.Json)
            setBody("""{"public_key":"key123"}""")
        }
        val res = client.get("/api/v1/keys/backup") { bearerAuth(testToken(userId)) }
        assertEquals(HttpStatusCode.NotFound, res.status)
    }
}
