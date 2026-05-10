@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.websocket

import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import me.soknight.minigram.chats.events.ChatEvent
import me.soknight.minigram.chats.plugin.appJson
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.koin.core.annotation.Single

@Single
class WebSocketConnectionManager {
    private val sessions = ConcurrentHashMap<Uuid, MutableSet<DefaultWebSocketSession>>()

    fun register(userId: Uuid, session: DefaultWebSocketSession) {
        sessions.getOrPut(userId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun unregister(userId: Uuid, session: DefaultWebSocketSession) {
        sessions[userId]?.remove(session)
    }

    suspend fun sendToUser(userId: Uuid, event: ChatEvent) {
        val payload = Frame.Text(appJson.encodeToString(event))
        sessions[userId]?.forEach { session -> runCatching { session.send(payload) } }
    }

    suspend fun sendToUsers(userIds: Collection<Uuid>, event: ChatEvent) {
        userIds.forEach { sendToUser(it, event) }
    }
}
