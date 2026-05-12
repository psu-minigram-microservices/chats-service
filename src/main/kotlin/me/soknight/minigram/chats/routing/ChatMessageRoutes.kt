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
import me.soknight.minigram.chats.service.ChatMessageService

@OptIn(ExperimentalKtorApi::class)
fun Route.chatMessageRoutes(messageService: ChatMessageService, profileClientFactory: ProfileClientFactory) {
    authenticate("jwt") {
        route("/api/v1/chats/{chat_id}/messages") {
            get {
                val profileClient = profileClientFactory.create(call.bearerToken())
                val page = call.queryInt("page", 0)
                val size = call.queryInt("size", 20)
                call.respond(messageService.getMessages(call.chatId(), profileClient.resolveMyProfileId(), profileClient, page, size))
            }.describe {
                summary = "Get messages"
                description = "Returns a paginated list of messages in the chat, newest first."
                parameters {
                    path("chat_id") { description = "Chat ID" }
                    query("page") {
                        description = "Page number (0-based)"
                        required = false
                    }
                    query("size") {
                        description = "Number of items per page"
                        required = false
                    }
                }
                responses {
                    HttpStatusCode.OK { description = "Paginated list of messages" }
                    HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                    HttpStatusCode.NotFound { description = "Chat not found" }
                }
            }
            post {
                val profileClient = profileClientFactory.create(call.bearerToken())
                val dto = messageService.sendMessage(call.chatId(), call.receive<SendMessageRequest>(), profileClient.resolveMyProfileId(), profileClient)
                call.respond(HttpStatusCode.Created, dto)
            }.describe {
                summary = "Send message"
                description = "Sends a message to the chat. Set `encrypted: true` and pass Base64-encoded ciphertext as `content` for end-to-end encrypted messages."
                parameters {
                    path("chat_id") { description = "Chat ID" }
                }
                responses {
                    HttpStatusCode.Created { description = "Message sent" }
                    HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                    HttpStatusCode.NotFound { description = "Chat not found" }
                }
            }
            route("/{message_id}") {
                get {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    call.respond(messageService.getMessage(call.chatId(), call.messageId(), profileClient.resolveMyProfileId(), profileClient))
                }.describe {
                    summary = "Get message"
                    description = "Returns a specific message by ID."
                    parameters {
                        path("chat_id") { description = "Chat ID" }
                        path("message_id") { description = "Message ID" }
                    }
                    responses {
                        HttpStatusCode.OK { description = "Message details" }
                        HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                        HttpStatusCode.NotFound { description = "Chat or message not found" }
                    }
                }
                patch {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    call.respond(messageService.editMessage(call.chatId(), call.messageId(), call.receive<EditMessageRequest>(), profileClient.resolveMyProfileId(), profileClient))
                }.describe {
                    summary = "Edit message"
                    description = "Partially updates a message. Only the sender can edit their own messages."
                    parameters {
                        path("chat_id") { description = "Chat ID" }
                        path("message_id") { description = "Message ID" }
                    }
                    responses {
                        HttpStatusCode.OK { description = "Updated message" }
                        HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                        HttpStatusCode.NotFound { description = "Chat or message not found" }
                    }
                }
                put {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    call.respond(messageService.editMessage(call.chatId(), call.messageId(), call.receive<EditMessageRequest>(), profileClient.resolveMyProfileId(), profileClient))
                }.describe {
                    summary = "Replace message"
                    description = "Full replacement of message content. Only the sender can edit their own messages."
                    parameters {
                        path("chat_id") { description = "Chat ID" }
                        path("message_id") { description = "Message ID" }
                    }
                    responses {
                        HttpStatusCode.OK { description = "Updated message" }
                        HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                        HttpStatusCode.NotFound { description = "Chat or message not found" }
                    }
                }
                delete {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    messageService.deleteMessage(call.chatId(), call.messageId(), profileClient.resolveMyProfileId())
                    call.respond(HttpStatusCode.NoContent)
                }.describe {
                    summary = "Delete message"
                    description = "Permanently deletes a message. Only the sender can delete their own messages."
                    parameters {
                        path("chat_id") { description = "Chat ID" }
                        path("message_id") { description = "Message ID" }
                    }
                    responses {
                        HttpStatusCode.NoContent { description = "Message deleted" }
                        HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                        HttpStatusCode.NotFound { description = "Chat or message not found" }
                    }
                }
            }
        }.describe { tag("Chat Messages") }
    }
}

private fun RoutingCall.messageId(): Long =
    parameters["message_id"]?.toLongOrNull() ?: throw ValidationException("message_id must be a number")
