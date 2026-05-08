package me.soknight.minigram.chats.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChatType { SAVED, DIRECT, GROUP }

@Serializable
enum class ChatMemberRole { OWNER, MEMBER }

@Serializable
enum class RelationStatus { NONE, FRIEND, BLOCKED }

enum class RelationType { INCOMING, OUTGOING }
