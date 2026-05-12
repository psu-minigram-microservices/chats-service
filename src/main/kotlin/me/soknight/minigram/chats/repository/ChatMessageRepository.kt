@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.repository

import me.soknight.minigram.chats.domain.ChatMessageRow
import me.soknight.minigram.chats.model.ChatMessagesTable
import me.soknight.minigram.chats.plugin.dbQuery
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.koin.core.annotation.Single

@Single
class ChatMessageRepository {

    suspend fun countByChatId(chatId: Long): Int = dbQuery {
        ChatMessagesTable.selectAll()
            .where { ChatMessagesTable.chatId eq chatId }
            .count().toInt()
    }

    suspend fun findByChatId(chatId: Long, page: Int, size: Int): List<ChatMessageRow> = dbQuery {
        ChatMessagesTable.selectAll()
            .where { ChatMessagesTable.chatId eq chatId }
            .orderBy(ChatMessagesTable.messageId, SortOrder.DESC)
            .limit(size).offset((page * size).toLong())
            .map { it.toRow() }
    }

    suspend fun findById(chatId: Long, messageId: Long): ChatMessageRow? = dbQuery {
        ChatMessagesTable.selectAll()
            .where { (ChatMessagesTable.chatId eq chatId) and (ChatMessagesTable.messageId eq messageId) }
            .singleOrNull()?.toRow()
    }

    suspend fun findLastMessageIdExcluding(chatId: Long, excludedMessageId: Long): Long? = dbQuery {
        ChatMessagesTable.selectAll()
            .where { (ChatMessagesTable.chatId eq chatId) and (ChatMessagesTable.messageId neq excludedMessageId) }
            .orderBy(ChatMessagesTable.messageId, SortOrder.DESC)
            .limit(1)
            .singleOrNull()?.get(ChatMessagesTable.messageId)
    }

    suspend fun insert(chatId: Long, messageId: Long, senderId: Uuid, content: String, encrypted: Boolean = false): ChatMessageRow = dbQuery {
        val now = Clock.System.now()
        ChatMessagesTable.insert {
            it[ChatMessagesTable.chatId]     = chatId
            it[ChatMessagesTable.messageId]  = messageId
            it[ChatMessagesTable.senderId]   = senderId
            it[ChatMessagesTable.content]    = content
            it[ChatMessagesTable.encrypted]  = encrypted
            it[ChatMessagesTable.createdAt]  = now
            it[ChatMessagesTable.updatedAt]  = now
        }
        ChatMessageRow(chatId, messageId, senderId, content, encrypted, now, now)
    }

    suspend fun updateContent(chatId: Long, messageId: Long, content: String): Unit = dbQuery {
        ChatMessagesTable.update({
            (ChatMessagesTable.chatId eq chatId) and (ChatMessagesTable.messageId eq messageId)
        }) {
            it[ChatMessagesTable.content]   = content
            it[ChatMessagesTable.updatedAt] = Clock.System.now()
        }
    }

    suspend fun delete(chatId: Long, messageId: Long): Unit = dbQuery {
        ChatMessagesTable.deleteWhere {
            (ChatMessagesTable.chatId eq chatId) and (ChatMessagesTable.messageId eq messageId)
        }
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toRow() = ChatMessageRow(
        chatId    = this[ChatMessagesTable.chatId],
        messageId = this[ChatMessagesTable.messageId],
        senderId  = this[ChatMessagesTable.senderId],
        content   = this[ChatMessagesTable.content],
        encrypted = this[ChatMessagesTable.encrypted],
        createdAt = this[ChatMessagesTable.createdAt],
        updatedAt = this[ChatMessagesTable.updatedAt]
    )
}
