@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.model

import me.soknight.minigram.chats.domain.ChatRow
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi

object ChatsTable : LongIdTable("chats") {
    val type          = varchar("type", 64)
    val title         = varchar("title", 255).nullable()
    val ownerId       = uuid("owner_id")
    val messageSeq    = long("message_sequence").default(0)
    val lastMessageId = long("last_message_id").nullable()
    val createdAt     = timestamp("created_at")
    val updatedAt     = timestamp("updated_at")
}

class ChatEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ChatEntity>(ChatsTable)

    var type          by ChatsTable.type
    var title         by ChatsTable.title
    var ownerId       by ChatsTable.ownerId
    var messageSeq    by ChatsTable.messageSeq
    var lastMessageId by ChatsTable.lastMessageId
    var createdAt     by ChatsTable.createdAt
    var updatedAt     by ChatsTable.updatedAt

    fun toDomain() = ChatRow(
        id              = id.value,
        type            = ChatType.valueOf(type),
        title           = title,
        ownerId         = ownerId,
        messageSequence = messageSeq,
        lastMessageId   = lastMessageId,
        createdAt       = createdAt,
        updatedAt       = updatedAt
    )
}

object ChatMembersTable : Table("chat_members") {
    val chatId   = long("chat_id").references(ChatsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId   = uuid("user_id")
    val role     = varchar("role", 16)
    val joinedAt = timestamp("joined_at")
    override val primaryKey = PrimaryKey(chatId, userId)
}

object ChatMessagesTable : Table("chat_messages") {
    val chatId    = long("chat_id").references(ChatsTable.id, onDelete = ReferenceOption.CASCADE)
    val messageId = long("message_id")
    val senderId  = uuid("sender_id")
    val content   = varchar("content", 4000)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(chatId, messageId)
}
