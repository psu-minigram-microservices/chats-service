@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.domain

import me.soknight.minigram.chats.model.ChatMemberRole
import me.soknight.minigram.chats.model.ChatType
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ChatRow(
    val id: Long,
    val type: ChatType,
    val title: String?,
    val ownerId: Uuid,
    val messageSequence: Long,
    val lastMessageId: Long?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class ChatMemberRow(
    val chatId: Long,
    val userId: Uuid,
    val role: ChatMemberRole,
    val joinedAt: Instant
)

data class ChatMessageRow(
    val chatId: Long,
    val messageId: Long,
    val senderId: Uuid,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
