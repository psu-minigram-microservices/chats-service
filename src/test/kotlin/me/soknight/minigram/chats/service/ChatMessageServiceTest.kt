@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.service

import io.mockk.*
import kotlinx.coroutines.runBlocking
import me.soknight.minigram.chats.domain.ChatMessageRow
import me.soknight.minigram.chats.dto.request.EditMessageRequest
import me.soknight.minigram.chats.dto.request.SendMessageRequest
import me.soknight.minigram.chats.exception.AccessDeniedException
import me.soknight.minigram.chats.exception.ValidationException
import me.soknight.minigram.chats.mockProfileClient
import me.soknight.minigram.chats.repository.ChatMemberRepository
import me.soknight.minigram.chats.repository.ChatMessageRepository
import me.soknight.minigram.chats.repository.ChatRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.test.*

class ChatMessageServiceTest {
    private val chatRepo    = mockk<ChatRepository>()
    private val memberRepo  = mockk<ChatMemberRepository>()
    private val messageRepo = mockk<ChatMessageRepository>()
    private val mapper      = mockk<ChatDtoMapper>()
    private val publisher   = mockk<ChatEventPublisher>(relaxed = true)
    private val service     = ChatMessageService(chatRepo, memberRepo, messageRepo, mapper, publisher)
    private val senderId    = Uuid.random()
    private val client      = mockProfileClient(senderId)

    private fun message(sender: Uuid = senderId) =
        ChatMessageRow(1L, 5L, sender, "hello", Clock.System.now(), Clock.System.now())

    @Test fun `sendMessage blank content throws ValidationException`() = runBlocking {
        coEvery { chatRepo.findAccessibleById(any(), any()) } returns mockk(relaxed = true)
        coEvery { memberRepo.existsById(any(), any()) } returns true
        assertFailsWith<ValidationException> {
            service.sendMessage(1L, SendMessageRequest("  "), senderId, client)
        }
    }

    @Test fun `editMessage by non-sender throws AccessDeniedException`() = runBlocking {
        coEvery { messageRepo.findById(1L, 5L) } returns message(Uuid.random())
        assertFailsWith<AccessDeniedException> {
            service.editMessage(1L, 5L, EditMessageRequest("new"), senderId, client)
        }
    }

    @Test fun `deleteMessage by non-sender throws AccessDeniedException`() = runBlocking {
        coEvery { messageRepo.findById(1L, 5L) } returns message(Uuid.random())
        assertFailsWith<AccessDeniedException> { service.deleteMessage(1L, 5L, senderId) }
    }
}
