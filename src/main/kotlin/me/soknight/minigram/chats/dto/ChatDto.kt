@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.dto

import kotlinx.serialization.Serializable
import me.soknight.minigram.chats.model.ChatMemberRole
import me.soknight.minigram.chats.model.ChatType
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class ChatDto(
    val id: Long,
    val type: ChatType,
    val title: String?,
    val ownerId: Uuid,
    val members: List<ChatMemberDto>,
    val lastMessageId: Long?,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class ChatMemberDto(
    val profileId: Uuid,
    val name: String,
    val photoUrl: String?,
    val role: ChatMemberRole,
    val joinedAt: Instant
)

@Serializable
data class ChatMessageDto(
    val id: Long,
    val chat: ChatDto,
    val sender: ChatMemberDto,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
