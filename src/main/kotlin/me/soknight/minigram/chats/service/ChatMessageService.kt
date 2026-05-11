@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.service

import me.soknight.minigram.chats.client.ProfileClient
import me.soknight.minigram.chats.dto.ChatMessageDto
import me.soknight.minigram.chats.dto.request.EditMessageRequest
import me.soknight.minigram.chats.dto.request.SendMessageRequest
import me.soknight.minigram.chats.events.ChatEvent
import me.soknight.minigram.chats.exception.*
import me.soknight.minigram.chats.model.ChatType
import me.soknight.minigram.chats.model.RelationStatus
import me.soknight.minigram.chats.model.RelationType
import me.soknight.minigram.chats.plugin.appJson
import me.soknight.minigram.chats.repository.ChatMemberRepository
import me.soknight.minigram.chats.repository.ChatMessageRepository
import me.soknight.minigram.chats.repository.ChatRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.koin.core.annotation.Single

@Single
class ChatMessageService(
    private val chatRepository: ChatRepository,
    private val memberRepository: ChatMemberRepository,
    private val messageRepository: ChatMessageRepository,
    private val dtoMapper: ChatDtoMapper,
    private val eventPublisher: ChatEventPublisher
) {
    suspend fun getMessages(chatId: Long, userId: Uuid, profileClient: ProfileClient, page: Int, size: Int): List<ChatMessageDto> {
        if (!memberRepository.existsById(chatId, userId)) throw MemberNotFoundException(chatId, userId)
        return messageRepository.findByChatId(chatId, page, size).map { dtoMapper.toChatMessageDto(it, profileClient) }
    }

    suspend fun getMessage(chatId: Long, messageId: Long, userId: Uuid, profileClient: ProfileClient): ChatMessageDto {
        if (!memberRepository.existsById(chatId, userId)) throw MemberNotFoundException(chatId, userId)
        val msg = messageRepository.findById(chatId, messageId) ?: throw MessageNotFoundException(chatId, messageId)
        return dtoMapper.toChatMessageDto(msg, profileClient)
    }

    suspend fun sendMessage(chatId: Long, request: SendMessageRequest, userId: Uuid, profileClient: ProfileClient): ChatMessageDto {
        if (request.content.isBlank()) throw ValidationException("content must not be blank")
        if (request.content.length > 4000) throw ValidationException("content must not exceed 4000 characters")
        val chat = chatRepository.findAccessibleById(chatId, userId) ?: throw ChatNotFoundException(chatId)

        if (chat.type == ChatType.DIRECT) {
            val recipientId = memberRepository.findUserIdsByChatId(chatId).firstOrNull { it != userId }
            if (recipientId != null) {
                val relation = profileClient.getRelation(recipientId, RelationType.OUTGOING)
                if (relation?.status != RelationStatus.FRIEND) throw RelationNotAcceptedException()
            }
        }

        val messageId = chatRepository.incrementMessageSequence(chatId)
        val message   = messageRepository.insert(chatId, messageId, userId, request.content)
        chatRepository.updateLastMessageId(chatId, messageId)
        val dto = dtoMapper.toChatMessageDto(message, profileClient)
        eventPublisher.publish(chatId, ChatEvent(
            ChatEvent.Type.MESSAGE_SENT, chatId,
            appJson.encodeToJsonElement(ChatMessageDto.serializer(), dto)
        ))
        return dto
    }

    suspend fun editMessage(chatId: Long, messageId: Long, request: EditMessageRequest, userId: Uuid, profileClient: ProfileClient): ChatMessageDto {
        if (request.content.isBlank()) throw ValidationException("content must not be blank")
        if (request.content.length > 4000) throw ValidationException("content must not exceed 4000 characters")
        val message = messageRepository.findById(chatId, messageId) ?: throw MessageNotFoundException(chatId, messageId)
        if (message.senderId != userId) throw AccessDeniedException()
        messageRepository.updateContent(chatId, messageId, request.content)
        val updated = messageRepository.findById(chatId, messageId)!!
        val dto     = dtoMapper.toChatMessageDto(updated, profileClient)
        eventPublisher.publish(chatId, ChatEvent(
            ChatEvent.Type.MESSAGE_EDITED, chatId,
            appJson.encodeToJsonElement(ChatMessageDto.serializer(), dto)
        ))
        return dto
    }

    suspend fun deleteMessage(chatId: Long, messageId: Long, userId: Uuid) {
        val message = messageRepository.findById(chatId, messageId) ?: throw MessageNotFoundException(chatId, messageId)
        if (message.senderId != userId) throw AccessDeniedException()
        val chat = chatRepository.findById(chatId)!!
        messageRepository.delete(chatId, messageId)
        if (chat.lastMessageId == messageId) {
            val newLastId = messageRepository.findLastMessageIdExcluding(chatId, messageId)
            chatRepository.updateLastMessageId(chatId, newLastId)
        }
        eventPublisher.publish(chatId, ChatEvent(ChatEvent.Type.MESSAGE_DELETED, chatId))
    }
}
