@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.service

import me.soknight.minigram.chats.client.ProfileClient
import me.soknight.minigram.chats.domain.ChatMemberRow
import me.soknight.minigram.chats.domain.ChatMessageRow
import me.soknight.minigram.chats.domain.ChatRow
import me.soknight.minigram.chats.dto.ChatDto
import me.soknight.minigram.chats.dto.ChatMemberDto
import me.soknight.minigram.chats.dto.ChatMessageDto
import me.soknight.minigram.chats.dto.ProfileDto
import me.soknight.minigram.chats.repository.ChatMemberRepository
import me.soknight.minigram.chats.repository.ChatRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.koin.core.annotation.Single

@Single
class ChatDtoMapper(
    private val chatRepository: ChatRepository,
    private val memberRepository: ChatMemberRepository
) {
    suspend fun toChatDto(chat: ChatRow, profileClient: ProfileClient): ChatDto {
        val members = memberRepository.findByChatId(chat.id, 0, 200)
        val cache   = mutableMapOf<Uuid, ProfileDto>()
        return ChatDto(
            id            = chat.id,
            type          = chat.type,
            title         = chat.title,
            ownerId       = chat.ownerId,
            members       = members.map { toChatMemberDto(it, profileClient, cache) },
            lastMessageId = chat.lastMessageId,
            createdAt     = chat.createdAt,
            updatedAt     = chat.updatedAt
        )
    }

    suspend fun toChatMemberDto(
        member: ChatMemberRow,
        profileClient: ProfileClient,
        cache: MutableMap<Uuid, ProfileDto> = mutableMapOf()
    ): ChatMemberDto {
        val profile = cache.getOrPut(member.userId) { profileClient.getProfile(member.userId) }
        return ChatMemberDto(member.userId, profile.name, profile.photoUrl, member.role, member.joinedAt)
    }

    suspend fun toChatMessageDto(message: ChatMessageRow, profileClient: ProfileClient): ChatMessageDto {
        val chat   = chatRepository.findById(message.chatId) ?: error("Chat ${message.chatId} not found")
        val cache  = mutableMapOf<Uuid, ProfileDto>()
        val chatDto = toChatDto(chat, profileClient)
        val senderRow = memberRepository.findById(message.chatId, message.senderId)
            ?: error("Sender ${message.senderId} not found in chat ${message.chatId}")
        return ChatMessageDto(
            id        = message.messageId,
            chat      = chatDto,
            sender    = toChatMemberDto(senderRow, profileClient, cache),
            content   = message.content,
            createdAt = message.createdAt,
            updatedAt = message.updatedAt
        )
    }
}
