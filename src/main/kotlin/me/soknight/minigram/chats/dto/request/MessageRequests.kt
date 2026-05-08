package me.soknight.minigram.chats.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequest(val content: String)

@Serializable
data class EditMessageRequest(val content: String)
