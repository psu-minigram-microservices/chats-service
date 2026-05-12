package me.soknight.minigram.chats.routing

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi
import me.soknight.minigram.chats.client.ProfileClient
import me.soknight.minigram.chats.client.ProfileClientFactory
import me.soknight.minigram.chats.dto.request.CreateChatRequest
import me.soknight.minigram.chats.dto.request.EditChatRequest
import me.soknight.minigram.chats.exception.ValidationException
import me.soknight.minigram.chats.service.ChatService
import kotlin.uuid.Uuid

@OptIn(ExperimentalKtorApi::class)
fun Route.chatRoutes(chatService: ChatService, profileClientFactory: ProfileClientFactory) {
    authenticate("jwt") {
        route("/api/v1/chats") {
            get {
                val profileClient = profileClientFactory.create(call.bearerToken())
                val profileId = profileClient.resolveMyProfileId()
                val page      = call.queryInt("page", 0)
                val size      = call.queryInt("size", 20)
                call.respond(chatService.getChats(profileId, profileClient, page, size))
            }.describe {
                summary = "Get my chats"
                description = "Returns a paginated list of all chats the current user is a member of."
                parameters {
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
                    HttpStatusCode.OK { description = "Paginated list of chats" }
                    HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                }
            }
            post {
                val profileClient = profileClientFactory.create(call.bearerToken())
                val dto = chatService.createChat(
                    call.receive<CreateChatRequest>(),
                    profileClient.resolveMyProfileId(),
                    profileClient
                )
                call.respond(HttpStatusCode.Created, dto)
            }.describe {
                summary = "Create a chat"
                description = """
                    Creates a new chat of the specified type:
                    - `saved` — personal notes chat, no other members needed
                    - `direct` — one-on-one chat, provide the other user's profile ID in `memberIds`
                    - `group` — group chat, optionally provide initial `memberIds` and a `title`
                """.trimIndent()
                responses {
                    HttpStatusCode.Created { description = "Chat created" }
                    HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                }
            }
            route("/{chat_id}") {
                get {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    call.respond(chatService.getChat(call.chatId(), profileClient.resolveMyProfileId(), profileClient))
                }.describe {
                    summary = "Get chat"
                    description = "Returns details of a specific chat. The current user must be a member."
                    parameters {
                        path("chat_id") { description = "Chat ID" }
                    }
                    responses {
                        HttpStatusCode.OK { description = "Chat details" }
                        HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                        HttpStatusCode.NotFound { description = "Chat not found" }
                    }
                }
                patch {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    call.respond(chatService.editChat(call.chatId(), call.receive<EditChatRequest>(), profileClient.resolveMyProfileId(), profileClient))
                }.describe {
                    summary = "Update chat"
                    description = "Partially updates chat properties. Only the owner can edit a chat."
                    parameters {
                        path("chat_id") { description = "Chat ID" }
                    }
                    responses {
                        HttpStatusCode.OK { description = "Updated chat" }
                        HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                        HttpStatusCode.NotFound { description = "Chat not found" }
                    }
                }
                put {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    call.respond(chatService.editChat(call.chatId(), call.receive<EditChatRequest>(), profileClient.resolveMyProfileId(), profileClient))
                }.describe {
                    summary = "Replace chat"
                    description = "Full replacement of chat properties. Only the owner can edit a chat."
                    parameters {
                        path("chat_id") { description = "Chat ID" }
                    }
                    responses {
                        HttpStatusCode.OK { description = "Updated chat" }
                        HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                        HttpStatusCode.NotFound { description = "Chat not found" }
                    }
                }
                delete {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    chatService.deleteChat(call.chatId(), profileClient.resolveMyProfileId())
                    call.respond(HttpStatusCode.NoContent)
                }.describe {
                    summary = "Delete chat"
                    description = "Permanently deletes the chat and all its messages. Only the owner can delete a chat."
                    parameters {
                        path("chat_id") { description = "Chat ID" }
                    }
                    responses {
                        HttpStatusCode.NoContent { description = "Chat deleted" }
                        HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                        HttpStatusCode.NotFound { description = "Chat not found" }
                    }
                }
            }
        }.describe { tag("Chats") }
    }
}

internal suspend fun ProfileClient.resolveMyProfileIdOrNull(): Uuid? = try {
    resolveMyProfileId()
} catch (e: Exception) { null }

internal fun RoutingCall.chatId(): Long =
    parameters["chat_id"]?.toLongOrNull() ?: throw ValidationException("chat_id must be a number")

internal fun RoutingCall.bearerToken(): String =
    request.authorization()?.removePrefix("Bearer ") ?: error("Missing Authorization header")

internal fun RoutingCall.queryInt(name: String, default: Int): Int =
    request.queryParameters[name]?.toIntOrNull() ?: default
