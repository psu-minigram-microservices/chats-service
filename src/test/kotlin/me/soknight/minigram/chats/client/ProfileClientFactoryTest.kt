package me.soknight.minigram.chats.client

import io.mockk.mockk
import me.soknight.minigram.chats.config.ServicesConfig
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

class ProfileClientFactoryTest {
    private val factory = ProfileClientFactory(
        httpClient     = mockk(relaxed = true),
        servicesConfig = ServicesConfig(profileUrl = "http://profiles")
    )

    @Test
    fun `create returns a ProfileClient`() {
        assertNotNull(factory.create("any-token"))
    }

    @Test
    fun `create returns a new instance each time`() {
        val client1 = factory.create("token-a")
        val client2 = factory.create("token-b")
        assertNotSame(client1, client2)
    }
}
