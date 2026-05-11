package me.soknight.minigram.chats.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ChatType {
    @SerialName("saved") SAVED,
    @SerialName("direct") DIRECT,
    @SerialName("group") GROUP
}

@Serializable
enum class ChatMemberRole {
    @SerialName("owner") OWNER,
    @SerialName("member") MEMBER
}

@Serializable
enum class RelationStatus {
    @SerialName("none") NONE,
    @SerialName("friend") FRIEND,
    @SerialName("blocked") BLOCKED
}

enum class RelationType { INCOMING, OUTGOING }
