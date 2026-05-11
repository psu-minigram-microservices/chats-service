@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.websocket

import io.ktor.websocket.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import me.soknight.minigram.chats.events.ChatEvent
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.test.*

class WebSocketConnectionManagerTest {
    private val manager = WebSocketConnectionManager()
    private val userId = Uuid.random()
    private val event = ChatEvent(ChatEvent.Type.CHAT_CREATED, 1L)

    private fun mockSession() = mockk<DefaultWebSocketSession>(relaxed = true)

    @Test fun `sendToUser delivers frame after register`() = runBlocking {
        val session = mockSession()
        manager.register(userId, session)
        manager.sendToUser(userId, event)
        coVerify(exactly = 1) { session.send(any<Frame.Text>()) }
    }

    @Test fun `sendToUser does nothing after unregister`() = runBlocking {
        val session = mockSession()
        manager.register(userId, session)
        manager.unregister(userId, session)
        manager.sendToUser(userId, event)
        coVerify(exactly = 0) { session.send(any<Frame>()) }
    }

    @Test fun `sendToUser ignores unknown user`() = runBlocking {
        manager.sendToUser(Uuid.random(), event)
    }

    @Test fun `sendToUsers delivers to all specified users`() = runBlocking {
        val session1 = mockSession()
        val session2 = mockSession()
        val userId2 = Uuid.random()
        manager.register(userId, session1)
        manager.register(userId2, session2)
        manager.sendToUsers(listOf(userId, userId2), event)
        coVerify(exactly = 1) { session1.send(any<Frame.Text>()) }
        coVerify(exactly = 1) { session2.send(any<Frame.Text>()) }
    }

    @Test fun `multiple sessions for same user all receive frame`() = runBlocking {
        val session1 = mockSession()
        val session2 = mockSession()
        manager.register(userId, session1)
        manager.register(userId, session2)
        manager.sendToUser(userId, event)
        coVerify(exactly = 1) { session1.send(any<Frame.Text>()) }
        coVerify(exactly = 1) { session2.send(any<Frame.Text>()) }
    }
}
