@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.service

import io.mockk.*
import kotlinx.coroutines.runBlocking
import me.soknight.minigram.chats.domain.ChatRow
import me.soknight.minigram.chats.dto.ChatDto
import me.soknight.minigram.chats.dto.request.CreateChatRequest
import me.soknight.minigram.chats.exception.InvalidChatMembersException
import me.soknight.minigram.chats.exception.InvalidChatTitleException
import me.soknight.minigram.chats.mockProfileClient
import me.soknight.minigram.chats.model.ChatMemberRole
import me.soknight.minigram.chats.model.ChatType
import me.soknight.minigram.chats.repository.ChatMemberRepository
import me.soknight.minigram.chats.repository.ChatRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.test.*

class ChatServiceTest {
    private val chatRepo   = mockk<ChatRepository>()
    private val memberRepo = mockk<ChatMemberRepository>()
    private val mapper     = mockk<ChatDtoMapper>()
    private val publisher  = mockk<ChatEventPublisher>(relaxed = true)
    private val service    = ChatService(chatRepo, memberRepo, mapper, publisher)
    private val userId     = Uuid.random()
    private val client     = mockProfileClient(userId)

    private fun chatRow(type: ChatType) = ChatRow(1L, type, null, userId, 0, null, Clock.System.now(), Clock.System.now())

    @Test fun `createChat SAVED succeeds with no memberIds`() = runBlocking {
        coEvery { chatRepo.insert(ChatType.SAVED, null, userId) } returns chatRow(ChatType.SAVED)
        coEvery { memberRepo.insert(1L, userId, ChatMemberRole.OWNER) } returns mockk()
        coEvery { memberRepo.findByChatId(1L, 0, 200) } returns emptyList()
        coEvery { mapper.toChatDto(any(), any()) } returns ChatDto(1L, ChatType.SAVED, null, userId, emptyList(), null, Clock.System.now(), Clock.System.now())
        service.createChat(CreateChatRequest(ChatType.SAVED), userId, client)
        coVerify { chatRepo.insert(ChatType.SAVED, null, userId) }
    }

    @Test fun `createChat SAVED fails when memberIds provided`() = runBlocking {
        assertFailsWith<InvalidChatMembersException> {
            service.createChat(CreateChatRequest(ChatType.SAVED, memberIds = listOf(Uuid.random())), userId, client)
        }
    }

    @Test fun `createChat DIRECT fails with 0 memberIds`() = runBlocking {
        assertFailsWith<InvalidChatMembersException> {
            service.createChat(CreateChatRequest(ChatType.DIRECT), userId, client)
        }
    }

    @Test fun `createChat DIRECT fails with 2 memberIds`() = runBlocking {
        assertFailsWith<InvalidChatMembersException> {
            service.createChat(CreateChatRequest(ChatType.DIRECT, memberIds = listOf(Uuid.random(), Uuid.random())), userId, client)
        }
    }

    @Test fun `createChat GROUP fails without title`() = runBlocking {
        assertFailsWith<InvalidChatTitleException> {
            service.createChat(CreateChatRequest(ChatType.GROUP, memberIds = listOf(Uuid.random())), userId, client)
        }
    }
}
