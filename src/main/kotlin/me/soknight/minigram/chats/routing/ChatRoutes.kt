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
            }
            post {
                val profileClient = profileClientFactory.create(call.bearerToken())
                val dto = chatService.createChat(
                    call.receive<CreateChatRequest>(),
                    profileClient.resolveMyProfileId(),
                    profileClient
                )
                call.respond(HttpStatusCode.Created, dto)
            }
            route("/{chat_id}") {
                get {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    call.respond(chatService.getChat(call.chatId(), profileClient.resolveMyProfileId(), profileClient))
                }
                patch {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    call.respond(chatService.editChat(call.chatId(), call.receive<EditChatRequest>(), profileClient.resolveMyProfileId(), profileClient))
                }
                put {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    call.respond(chatService.editChat(call.chatId(), call.receive<EditChatRequest>(), profileClient.resolveMyProfileId(), profileClient))
                }
                delete {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    chatService.deleteChat(call.chatId(), profileClient.resolveMyProfileId())
                    call.respond(HttpStatusCode.NoContent)
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
