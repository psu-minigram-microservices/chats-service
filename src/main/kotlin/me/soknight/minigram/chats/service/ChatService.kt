@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.service

import kotlinx.serialization.encodeToString
import me.soknight.minigram.chats.client.ProfileClient
import me.soknight.minigram.chats.dto.ChatDto
import me.soknight.minigram.chats.dto.request.CreateChatRequest
import me.soknight.minigram.chats.dto.request.EditChatRequest
import me.soknight.minigram.chats.events.ChatEvent
import me.soknight.minigram.chats.exception.*
import me.soknight.minigram.chats.model.ChatMemberRole
import me.soknight.minigram.chats.model.ChatType
import me.soknight.minigram.chats.model.RelationStatus
import me.soknight.minigram.chats.model.RelationType
import me.soknight.minigram.chats.plugin.appJson
import me.soknight.minigram.chats.repository.ChatMemberRepository
import me.soknight.minigram.chats.repository.ChatRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.koin.core.annotation.Single

@Single
class ChatService(
    private val chatRepository: ChatRepository,
    private val memberRepository: ChatMemberRepository,
    private val dtoMapper: ChatDtoMapper,
    private val eventPublisher: ChatEventPublisher
) {
    suspend fun getChats(userId: Uuid, profileClient: ProfileClient, page: Int, size: Int): List<ChatDto> =
        chatRepository.findAllByMemberId(userId, page, size).map { dtoMapper.toChatDto(it, profileClient) }

    suspend fun getChat(chatId: Long, userId: Uuid, profileClient: ProfileClient): ChatDto {
        val chat = chatRepository.findAccessibleById(chatId, userId) ?: throw ChatNotFoundException(chatId)
        return dtoMapper.toChatDto(chat, profileClient)
    }

    suspend fun createChat(request: CreateChatRequest, userId: Uuid, profileClient: ProfileClient): ChatDto {
        when (request.type) {
            ChatType.SAVED -> {
                if (request.memberIds.isNotEmpty())
                    throw InvalidChatMembersException("SAVED chats cannot have other members")
            }
            ChatType.DIRECT -> {
                if (request.memberIds.size != 1)
                    throw InvalidChatMembersException("DIRECT chats require exactly 1 other member")
                assertFriend(request.memberIds[0], profileClient)
            }
            ChatType.GROUP -> {
                if (request.memberIds.isEmpty())
                    throw InvalidChatMembersException("GROUP chats require at least 1 other member")
                if (request.title.isNullOrBlank()) throw InvalidChatTitleException()
                request.memberIds.forEach { assertFriend(it, profileClient) }
            }
        }

        val chat = chatRepository.insert(request.type, request.title, userId)
        memberRepository.insert(chat.id, userId, ChatMemberRole.OWNER)
        request.memberIds.forEach { memberRepository.insert(chat.id, it, ChatMemberRole.MEMBER) }

        val dto        = dtoMapper.toChatDto(chat, profileClient)
        val allMembers = listOf(userId) + request.memberIds
        eventPublisher.publishToUsers(allMembers, ChatEvent(
            ChatEvent.Type.CHAT_CREATED, chat.id,
            appJson.encodeToJsonElement(ChatDto.serializer(), dto)
        ))
        return dto
    }

    suspend fun editChat(chatId: Long, request: EditChatRequest, userId: Uuid, profileClient: ProfileClient): ChatDto {
        val chat = chatRepository.findAccessibleById(chatId, userId) ?: throw ChatNotFoundException(chatId)
        if (chat.ownerId != userId) throw AccessDeniedException()
        if (chat.type != ChatType.GROUP) throw ChatEditNotSupportedException()
        chatRepository.updateTitle(chatId, request.title)
        val updated = chatRepository.findById(chatId)!!
        val dto     = dtoMapper.toChatDto(updated, profileClient)
        eventPublisher.publish(chatId, ChatEvent(
            ChatEvent.Type.CHAT_UPDATED, chatId,
            appJson.encodeToJsonElement(ChatDto.serializer(), dto)
        ))
        return dto
    }

    suspend fun deleteChat(chatId: Long, userId: Uuid) {
        val chat    = chatRepository.findAccessibleById(chatId, userId) ?: throw ChatNotFoundException(chatId)
        if (chat.ownerId != userId) throw AccessDeniedException()
        val members = memberRepository.findUserIdsByChatId(chatId)
        eventPublisher.publishToUsers(members, ChatEvent(ChatEvent.Type.CHAT_DELETED, chatId))
        chatRepository.delete(chatId)
    }

    private suspend fun assertFriend(targetId: Uuid, profileClient: ProfileClient) {
        val relation = profileClient.getRelation(targetId, RelationType.OUTGOING)
        if (relation?.status != RelationStatus.FRIEND) throw RelationNotAcceptedException()
    }
}
