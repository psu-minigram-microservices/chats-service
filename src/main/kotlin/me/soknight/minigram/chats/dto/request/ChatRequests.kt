@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.dto.request

import kotlinx.serialization.Serializable
import me.soknight.minigram.chats.model.ChatType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class CreateChatRequest(
    val type: ChatType,
    val title: String? = null,
    val memberIds: List<Uuid> = emptyList()
)

@Serializable
data class EditChatRequest(val title: String? = null)
