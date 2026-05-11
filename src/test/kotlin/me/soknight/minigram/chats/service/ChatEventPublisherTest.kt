@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.service

import io.mockk.*
import kotlinx.coroutines.runBlocking
import me.soknight.minigram.chats.events.ChatEvent
import me.soknight.minigram.chats.repository.ChatMemberRepository
import me.soknight.minigram.chats.websocket.WebSocketConnectionManager
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.test.*

class ChatEventPublisherTest {
    private val connectionManager = mockk<WebSocketConnectionManager>(relaxed = true)
    private val memberRepo = mockk<ChatMemberRepository>()
    private val publisher = ChatEventPublisher(connectionManager, memberRepo)

    @Test fun `publish fetches members and sends to all`() = runBlocking {
        val userIds = listOf(Uuid.random(), Uuid.random())
        val event = ChatEvent(ChatEvent.Type.MESSAGE_SENT, 1L)
        coEvery { memberRepo.findUserIdsByChatId(1L) } returns userIds
        publisher.publish(1L, event)
        coVerify { connectionManager.sendToUsers(userIds, event) }
    }

    @Test fun `publish with no members sends to empty list`() = runBlocking {
        val event = ChatEvent(ChatEvent.Type.CHAT_DELETED, 2L)
        coEvery { memberRepo.findUserIdsByChatId(2L) } returns emptyList()
        publisher.publish(2L, event)
        coVerify { connectionManager.sendToUsers(emptyList(), event) }
    }

    @Test fun `publishToUsers sends to specified users`() = runBlocking {
        val userIds = listOf(Uuid.random(), Uuid.random())
        val event = ChatEvent(ChatEvent.Type.MEMBER_JOINED, 3L)
        publisher.publishToUsers(userIds, event)
        coVerify { connectionManager.sendToUsers(userIds, event) }
    }
}
