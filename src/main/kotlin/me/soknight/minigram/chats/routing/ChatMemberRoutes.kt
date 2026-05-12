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
            }.describe {
                summary = "Get members"
                description = "Returns a paginated list of all members in the chat."
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
                    HttpStatusCode.OK { description = "Paginated list of members" }
                    HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                    HttpStatusCode.NotFound { description = "Chat not found" }
                }
            }
            route("/me") {
                get {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    val profileId = profileClient.resolveMyProfileId()
                    call.respond(memberService.getMember(call.chatId(), profileId, profileId, profileClient))
                }.describe {
                    summary = "Get my membership"
                    description = "Returns the current user's membership details in this chat, including their role."
                    parameters {
                        path("chat_id") { description = "Chat ID" }
                    }
                    responses {
                        HttpStatusCode.OK { description = "Current user's member info" }
                        HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                        HttpStatusCode.NotFound { description = "Chat not found or user is not a member" }
                    }
                }
                delete {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    memberService.leaveChat(call.chatId(), profileClient.resolveMyProfileId())
                    call.respond(HttpStatusCode.NoContent)
                }.describe {
                    summary = "Leave chat"
                    description = "Removes the current user from the chat. The owner cannot leave — they must delete the chat first."
                    parameters {
                        path("chat_id") { description = "Chat ID" }
                    }
                    responses {
                        HttpStatusCode.NoContent { description = "Left the chat" }
                        HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                        HttpStatusCode.NotFound { description = "Chat not found" }
                    }
                }
            }
            route("/{member_id}") {
                post {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    memberService.inviteUser(call.chatId(), call.memberId(), profileClient.resolveMyProfileId(), profileClient)
                    call.respond(HttpStatusCode.NoContent)
                }.describe {
                    summary = "Invite user"
                    description = "Adds a user to the chat by their profile ID. Only the owner can invite new members."
                    parameters {
                        path("chat_id") { description = "Chat ID" }
                        path("member_id") { description = "Profile ID of the user to invite" }
                    }
                    responses {
                        HttpStatusCode.NoContent { description = "User invited" }
                        HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                        HttpStatusCode.NotFound { description = "Chat or user not found" }
                    }
                }
                get {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    call.respond(memberService.getMember(call.chatId(), call.memberId(), profileClient.resolveMyProfileId(), profileClient))
                }.describe {
                    summary = "Get member"
                    description = "Returns membership details for a specific user in the chat."
                    parameters {
                        path("chat_id") { description = "Chat ID" }
                        path("member_id") { description = "Profile ID of the member" }
                    }
                    responses {
                        HttpStatusCode.OK { description = "Member details" }
                        HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                        HttpStatusCode.NotFound { description = "Chat or member not found" }
                    }
                }
                delete {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    memberService.kickUser(call.chatId(), call.memberId(), profileClient.resolveMyProfileId())
                    call.respond(HttpStatusCode.NoContent)
                }.describe {
                    summary = "Kick user"
                    description = "Removes a user from the chat. Only the owner can kick members."
                    parameters {
                        path("chat_id") { description = "Chat ID" }
                        path("member_id") { description = "Profile ID of the member to kick" }
                    }
                    responses {
                        HttpStatusCode.NoContent { description = "User removed" }
                        HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                        HttpStatusCode.NotFound { description = "Chat or member not found" }
                    }
                }
            }
        }.describe { tag("Chat Members") }
    }
}

private fun RoutingCall.memberId(): Uuid =
    parameters["member_id"]?.let { runCatching { Uuid.parse(it) }.getOrNull() }
        ?: throw ValidationException("member_id must be a valid UUID")
