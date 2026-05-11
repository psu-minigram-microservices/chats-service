@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.service

import me.soknight.minigram.chats.client.ProfileClient
import me.soknight.minigram.chats.dto.ChatMemberDto
import me.soknight.minigram.chats.dto.PageDto
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
class ChatMemberService(
    private val chatRepository: ChatRepository,
    private val memberRepository: ChatMemberRepository,
    private val dtoMapper: ChatDtoMapper,
    private val eventPublisher: ChatEventPublisher
) {
    suspend fun getMembers(chatId: Long, userId: Uuid, profileClient: ProfileClient, page: Int, size: Int): PageDto<ChatMemberDto> {
        if (!memberRepository.existsById(chatId, userId)) throw MemberNotFoundException(chatId, userId)
        val content = memberRepository.findByChatId(chatId, page, size).map { dtoMapper.toChatMemberDto(it, profileClient) }
        val total   = memberRepository.countByChatId(chatId)
        return PageDto(content, page, size, total)
    }

    suspend fun getMember(chatId: Long, targetId: Uuid, callerId: Uuid, profileClient: ProfileClient): ChatMemberDto {
        if (!memberRepository.existsById(chatId, callerId)) throw MemberNotFoundException(chatId, callerId)
        val member = memberRepository.findById(chatId, targetId) ?: throw MemberNotFoundException(chatId, targetId)
        return dtoMapper.toChatMemberDto(member, profileClient)
    }

    suspend fun inviteUser(chatId: Long, profileId: Uuid, callerId: Uuid, profileClient: ProfileClient) {
        val chat = chatRepository.findAccessibleById(chatId, callerId) ?: throw ChatNotFoundException(chatId)
        if (chat.type != ChatType.GROUP) throw ChatInviteNotSupportedException()
        if (chat.ownerId != callerId) throw AccessDeniedException()
        if (memberRepository.existsById(chatId, profileId)) throw MemberAlreadyExistsException()
        val relation = profileClient.getRelation(profileId, RelationType.OUTGOING)
        if (relation?.status != RelationStatus.FRIEND) throw RelationNotAcceptedException()
        val member = memberRepository.insert(chatId, profileId, ChatMemberRole.MEMBER)
        val dto    = dtoMapper.toChatMemberDto(member, profileClient)
        eventPublisher.publish(chatId, ChatEvent(
            ChatEvent.Type.MEMBER_JOINED, chatId,
            appJson.encodeToJsonElement(ChatMemberDto.serializer(), dto)
        ))
    }

    suspend fun leaveChat(chatId: Long, userId: Uuid) {
        val chat = chatRepository.findAccessibleById(chatId, userId) ?: throw ChatNotFoundException(chatId)
        if (chat.type == ChatType.SAVED) throw CannotLeaveChatException()
        if (chat.ownerId == userId) throw OwnerCannotLeaveChatException()
        eventPublisher.publish(chatId, ChatEvent(ChatEvent.Type.MEMBER_LEFT, chatId))
        memberRepository.delete(chatId, userId)
    }

    suspend fun kickUser(chatId: Long, targetId: Uuid, callerId: Uuid) {
        val chat = chatRepository.findAccessibleById(chatId, callerId) ?: throw ChatNotFoundException(chatId)
        if (chat.type != ChatType.GROUP) throw ChatKickNotSupportedException()
        if (chat.ownerId != callerId) throw AccessDeniedException()
        if (targetId == callerId) throw CannotKickSelfException()
        if (!memberRepository.existsById(chatId, targetId)) throw MemberNotFoundException(chatId, targetId)
        eventPublisher.publish(chatId, ChatEvent(ChatEvent.Type.MEMBER_LEFT, chatId))
        memberRepository.delete(chatId, targetId)
    }
}
