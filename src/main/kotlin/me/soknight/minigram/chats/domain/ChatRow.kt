@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.domain

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
