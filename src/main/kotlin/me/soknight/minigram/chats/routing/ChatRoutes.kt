package me.soknight.minigram.chats.routing

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.soknight.minigram.chats.client.ProfileClientFactory
import me.soknight.minigram.chats.dto.request.CreateChatRequest
import me.soknight.minigram.chats.dto.request.EditChatRequest
import me.soknight.minigram.chats.exception.ValidationException
import me.soknight.minigram.chats.plugin.currentUserId
import me.soknight.minigram.chats.service.ChatService

fun Route.chatRoutes(chatService: ChatService, profileClientFactory: ProfileClientFactory) {
    authenticate("jwt") {
        route("/api/v1/chats") {
            get {
                val userId = call.currentUserId()
                val page   = call.queryInt("page", 0)
                val size   = call.queryInt("size", 20)
                call.respond(chatService.getChats(userId, profileClientFactory.create(call.bearerToken()), page, size))
            }
            post {
                val dto = chatService.createChat(
                    call.receive<CreateChatRequest>(),
                    call.currentUserId(),
                    profileClientFactory.create(call.bearerToken())
                )
                call.respond(HttpStatusCode.Created, dto)
            }
            route("/{chat_id}") {
                get {
                    call.respond(chatService.getChat(call.chatId(), call.currentUserId(), profileClientFactory.create(call.bearerToken())))
                }
                patch {
                    call.respond(chatService.editChat(call.chatId(), call.receive<EditChatRequest>(), call.currentUserId(), profileClientFactory.create(call.bearerToken())))
                }
                put {
                    call.respond(chatService.editChat(call.chatId(), call.receive<EditChatRequest>(), call.currentUserId(), profileClientFactory.create(call.bearerToken())))
                }
                delete {
                    chatService.deleteChat(call.chatId(), call.currentUserId())
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

internal fun RoutingCall.chatId(): Long =
    parameters["chat_id"]?.toLongOrNull() ?: throw ValidationException("chat_id must be a number")

internal fun RoutingCall.bearerToken(): String =
    request.authorization()?.removePrefix("Bearer ") ?: error("Missing Authorization header")

internal fun RoutingCall.queryInt(name: String, default: Int): Int =
    request.queryParameters[name]?.toIntOrNull() ?: default
