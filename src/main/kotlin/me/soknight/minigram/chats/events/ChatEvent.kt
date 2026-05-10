package me.soknight.minigram.chats.events

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

@Serializable
data class ChatEvent(
    val type: Type,
    val chatId: Long,
    val payload: JsonElement = JsonNull
) {
    enum class Type {
        CHAT_CREATED, CHAT_UPDATED, CHAT_DELETED,
        MESSAGE_SENT, MESSAGE_EDITED, MESSAGE_DELETED,
        MEMBER_JOINED, MEMBER_LEFT
    }
}
