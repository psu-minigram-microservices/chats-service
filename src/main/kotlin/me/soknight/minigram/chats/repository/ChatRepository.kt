@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.repository

import me.soknight.minigram.chats.domain.ChatRow
import me.soknight.minigram.chats.model.*
import me.soknight.minigram.chats.plugin.dbQuery
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ChatRepository {

    suspend fun findAllByMemberId(userId: Uuid, page: Int, size: Int): List<ChatRow> = dbQuery {
        ChatsTable.innerJoin(ChatMembersTable)
            .selectAll()
            .where { ChatMembersTable.userId eq userId }
            .orderBy(ChatsTable.updatedAt, SortOrder.DESC)
            .limit(size).offset((page * size).toLong())
            .map { ChatEntity.wrapRow(it).toDomain() }
    }

    suspend fun findById(chatId: Long): ChatRow? = dbQuery {
        ChatEntity.findById(chatId)?.toDomain()
    }

    suspend fun findAccessibleById(chatId: Long, userId: Uuid): ChatRow? = dbQuery {
        ChatsTable.innerJoin(ChatMembersTable)
            .selectAll()
            .where { (ChatsTable.id eq chatId) and (ChatMembersTable.userId eq userId) }
            .singleOrNull()
            ?.let { ChatEntity.wrapRow(it).toDomain() }
    }

    suspend fun insert(type: ChatType, title: String?, ownerId: Uuid): ChatRow = dbQuery {
        val now = Clock.System.now()
        ChatEntity.new {
            this.type          = type.name
            this.title         = title
            this.ownerId       = ownerId
            this.messageSeq    = 0
            this.lastMessageId = null
            this.createdAt     = now
            this.updatedAt     = now
        }.toDomain()
    }

    suspend fun updateTitle(chatId: Long, newTitle: String?): Unit = dbQuery {
        ChatsTable.update({ ChatsTable.id eq chatId }) {
            it[title]     = newTitle
            it[updatedAt] = Clock.System.now()
        }
    }

    suspend fun delete(chatId: Long): Unit = dbQuery {
        ChatEntity.findById(chatId)?.delete()
    }

    suspend fun incrementMessageSequence(chatId: Long): Long = dbQuery {
        val entity = ChatEntity.findById(chatId) ?: error("Chat $chatId not found")
        entity.messageSeq += 1
        entity.messageSeq
    }

    suspend fun updateLastMessageId(chatId: Long, messageId: Long?): Unit = dbQuery {
        ChatsTable.update({ ChatsTable.id eq chatId }) {
            it[lastMessageId] = messageId
            it[updatedAt]     = Clock.System.now()
        }
    }
}
