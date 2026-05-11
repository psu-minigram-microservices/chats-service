@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.routing

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi
import me.soknight.minigram.chats.client.ProfileClientFactory
import me.soknight.minigram.chats.exception.ValidationException
import me.soknight.minigram.chats.service.ChatMemberService
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalKtorApi::class)
fun Route.chatMemberRoutes(memberService: ChatMemberService, profileClientFactory: ProfileClientFactory) {
    authenticate("jwt") {
        route("/api/v1/chats/{chat_id}/members") {
            get {
                val profileClient = profileClientFactory.create(call.bearerToken())
                val page = call.queryInt("page", 0)
                val size = call.queryInt("size", 20)
                call.respond(memberService.getMembers(call.chatId(), profileClient.resolveMyProfileId(), profileClient, page, size))
            }
            route("/me") {
                get {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    val profileId = profileClient.resolveMyProfileId()
                    call.respond(memberService.getMember(call.chatId(), profileId, profileId, profileClient))
                }
                delete {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    memberService.leaveChat(call.chatId(), profileClient.resolveMyProfileId())
                    call.respond(HttpStatusCode.NoContent)
                }
            }
            route("/{member_id}") {
                post {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    memberService.inviteUser(call.chatId(), call.memberId(), profileClient.resolveMyProfileId(), profileClient)
                    call.respond(HttpStatusCode.NoContent)
                }
                get {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    call.respond(memberService.getMember(call.chatId(), call.memberId(), profileClient.resolveMyProfileId(), profileClient))
                }
                delete {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    memberService.kickUser(call.chatId(), call.memberId(), profileClient.resolveMyProfileId())
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }.describe { tag("Chat Members") }
    }
}

private fun RoutingCall.memberId(): Uuid =
    parameters["member_id"]?.let { runCatching { Uuid.parse(it) }.getOrNull() }
        ?: throw ValidationException("member_id must be a valid UUID")
