@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.repository

import me.soknight.minigram.chats.domain.ChatMemberRow
import me.soknight.minigram.chats.model.ChatMemberRole
import me.soknight.minigram.chats.model.ChatMembersTable
import me.soknight.minigram.chats.plugin.dbQuery
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.koin.core.annotation.Single

@Single
class ChatMemberRepository {

    suspend fun findUserIdsByChatId(chatId: Long): List<Uuid> = dbQuery {
        ChatMembersTable.selectAll()
            .where { ChatMembersTable.chatId eq chatId }
            .map { it[ChatMembersTable.userId] }
    }

    suspend fun findByChatId(chatId: Long, page: Int, size: Int): List<ChatMemberRow> = dbQuery {
        ChatMembersTable.selectAll()
            .where { ChatMembersTable.chatId eq chatId }
            .limit(size).offset((page * size).toLong())
            .map { it.toRow() }
    }

    suspend fun findById(chatId: Long, userId: Uuid): ChatMemberRow? = dbQuery {
        ChatMembersTable.selectAll()
            .where { (ChatMembersTable.chatId eq chatId) and (ChatMembersTable.userId eq userId) }
            .singleOrNull()?.toRow()
    }

    suspend fun existsById(chatId: Long, userId: Uuid): Boolean = dbQuery {
        ChatMembersTable.selectAll()
            .where { (ChatMembersTable.chatId eq chatId) and (ChatMembersTable.userId eq userId) }
            .count() > 0
    }

    suspend fun insert(chatId: Long, userId: Uuid, role: ChatMemberRole): ChatMemberRow = dbQuery {
        val now = Clock.System.now()
        ChatMembersTable.insert {
            it[ChatMembersTable.chatId]   = chatId
            it[ChatMembersTable.userId]   = userId
            it[ChatMembersTable.role]     = role.name
            it[ChatMembersTable.joinedAt] = now
        }
        ChatMemberRow(chatId, userId, role, now)
    }

    suspend fun delete(chatId: Long, userId: Uuid): Unit = dbQuery {
        ChatMembersTable.deleteWhere {
            (ChatMembersTable.chatId eq chatId) and (ChatMembersTable.userId eq userId)
        }
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toRow() = ChatMemberRow(
        chatId   = this[ChatMembersTable.chatId],
        userId   = this[ChatMembersTable.userId],
        role     = ChatMemberRole.valueOf(this[ChatMembersTable.role]),
        joinedAt = this[ChatMembersTable.joinedAt]
    )
}
