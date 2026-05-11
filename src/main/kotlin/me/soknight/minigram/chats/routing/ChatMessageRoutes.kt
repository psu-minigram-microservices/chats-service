package me.soknight.minigram.chats.routing

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi
import me.soknight.minigram.chats.client.ProfileClientFactory
import me.soknight.minigram.chats.dto.request.EditMessageRequest
import me.soknight.minigram.chats.dto.request.SendMessageRequest
import me.soknight.minigram.chats.exception.ValidationException
import me.soknight.minigram.chats.plugin.currentUserId
import me.soknight.minigram.chats.service.ChatMessageService

@OptIn(ExperimentalKtorApi::class)
fun Route.chatMessageRoutes(messageService: ChatMessageService, profileClientFactory: ProfileClientFactory) {
    authenticate("jwt") {
        route("/api/v1/chats/{chat_id}/messages") {
            get {
                val page = call.queryInt("page", 0)
                val size = call.queryInt("size", 20)
                call.respond(messageService.getMessages(call.chatId(), call.currentUserId(), profileClientFactory.create(call.bearerToken()), page, size))
            }
            post {
                val dto = messageService.sendMessage(call.chatId(), call.receive<SendMessageRequest>(), call.currentUserId(), profileClientFactory.create(call.bearerToken()))
                call.respond(HttpStatusCode.Created, dto)
            }
            route("/{message_id}") {
                get {
                    call.respond(messageService.getMessage(call.chatId(), call.messageId(), call.currentUserId(), profileClientFactory.create(call.bearerToken())))
                }
                patch {
                    call.respond(messageService.editMessage(call.chatId(), call.messageId(), call.receive<EditMessageRequest>(), call.currentUserId(), profileClientFactory.create(call.bearerToken())))
                }
                put {
                    call.respond(messageService.editMessage(call.chatId(), call.messageId(), call.receive<EditMessageRequest>(), call.currentUserId(), profileClientFactory.create(call.bearerToken())))
                }
                delete {
                    messageService.deleteMessage(call.chatId(), call.messageId(), call.currentUserId())
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }.describe { tag("Chat Messages") }
    }
}

private fun RoutingCall.messageId(): Long =
    parameters["message_id"]?.toLongOrNull() ?: throw ValidationException("message_id must be a number")
