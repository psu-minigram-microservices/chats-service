@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.routing

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.soknight.minigram.chats.client.ProfileClient
import me.soknight.minigram.chats.exception.ValidationException
import me.soknight.minigram.chats.plugin.currentUserId
import me.soknight.minigram.chats.service.ChatMemberService
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun Route.chatMemberRoutes(memberService: ChatMemberService, clientFactory: (String) -> ProfileClient) {
    authenticate("jwt") {
        route("/api/v1/chats/{chat_id}/members") {
            get {
                val page = call.queryInt("page", 0)
                val size = call.queryInt("size", 20)
                call.respond(memberService.getMembers(call.chatId(), call.currentUserId(), clientFactory(call.bearerToken()), page, size))
            }
            route("/me") {
                get {
                    val userId = call.currentUserId()
                    call.respond(memberService.getMember(call.chatId(), userId, userId, clientFactory(call.bearerToken())))
                }
                delete {
                    memberService.leaveChat(call.chatId(), call.currentUserId())
                    call.respond(HttpStatusCode.NoContent)
                }
            }
            route("/{member_id}") {
                post {
                    memberService.inviteUser(call.chatId(), call.memberId(), call.currentUserId(), clientFactory(call.bearerToken()))
                    call.respond(HttpStatusCode.NoContent)
                }
                get {
                    call.respond(memberService.getMember(call.chatId(), call.memberId(), call.currentUserId(), clientFactory(call.bearerToken())))
                }
                delete {
                    memberService.kickUser(call.chatId(), call.memberId(), call.currentUserId())
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

private fun RoutingCall.memberId(): Uuid =
    parameters["member_id"]?.let { runCatching { Uuid.parse(it) }.getOrNull() }
        ?: throw ValidationException("member_id must be a valid UUID")
