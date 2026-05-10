@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.service

import io.mockk.*
import kotlinx.coroutines.runBlocking
import me.soknight.minigram.chats.domain.ChatRow
import me.soknight.minigram.chats.exception.*
import me.soknight.minigram.chats.mockProfileClient
import me.soknight.minigram.chats.model.ChatType
import me.soknight.minigram.chats.repository.ChatMemberRepository
import me.soknight.minigram.chats.repository.ChatRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.test.*

class ChatMemberServiceTest {
    private val chatRepo   = mockk<ChatRepository>()
    private val memberRepo = mockk<ChatMemberRepository>()
    private val mapper     = mockk<ChatDtoMapper>()
    private val publisher  = mockk<ChatEventPublisher>(relaxed = true)
    private val service    = ChatMemberService(chatRepo, memberRepo, mapper, publisher)
    private val ownerId    = Uuid.random()
    private val client     = mockProfileClient(ownerId)

    private fun chat(type: ChatType) = ChatRow(1L, type, if (type == ChatType.GROUP) "G" else null, ownerId, 0, null, Clock.System.now(), Clock.System.now())

    @Test fun `leaveChat SAVED throws CannotLeaveChatException`() = runBlocking {
        coEvery { chatRepo.findAccessibleById(1L, ownerId) } returns chat(ChatType.SAVED)
        assertFailsWith<CannotLeaveChatException> { service.leaveChat(1L, ownerId) }
    }

    @Test fun `leaveChat as owner throws OwnerCannotLeaveChatException`() = runBlocking {
        coEvery { chatRepo.findAccessibleById(1L, ownerId) } returns chat(ChatType.GROUP)
        assertFailsWith<OwnerCannotLeaveChatException> { service.leaveChat(1L, ownerId) }
    }

    @Test fun `inviteUser non-GROUP throws ChatInviteNotSupportedException`() = runBlocking {
        coEvery { chatRepo.findAccessibleById(1L, ownerId) } returns chat(ChatType.SAVED)
        assertFailsWith<ChatInviteNotSupportedException> {
            service.inviteUser(1L, Uuid.random(), ownerId, client)
        }
    }

    @Test fun `kickUser non-owner throws AccessDeniedException`() = runBlocking {
        val memberId = Uuid.random()
        coEvery { chatRepo.findAccessibleById(1L, memberId) } returns chat(ChatType.GROUP)
        assertFailsWith<AccessDeniedException> { service.kickUser(1L, ownerId, memberId) }
    }
}
