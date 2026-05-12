package me.soknight.minigram.chats.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequest(
    val content: String,
    val encrypted: Boolean = false
)

@Serializable
data class EditMessageRequest(val content: String)
