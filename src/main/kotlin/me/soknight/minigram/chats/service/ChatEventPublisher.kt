@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.service

import me.soknight.minigram.chats.events.ChatEvent
import me.soknight.minigram.chats.repository.ChatMemberRepository
import me.soknight.minigram.chats.websocket.WebSocketConnectionManager
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ChatEventPublisher(
    private val connectionManager: WebSocketConnectionManager,
    private val memberRepository: ChatMemberRepository
) {
    suspend fun publish(chatId: Long, event: ChatEvent) {
        val userIds = memberRepository.findUserIdsByChatId(chatId)
        connectionManager.sendToUsers(userIds, event)
    }

    suspend fun publishToUsers(userIds: Collection<Uuid>, event: ChatEvent) {
        connectionManager.sendToUsers(userIds, event)
    }
}
