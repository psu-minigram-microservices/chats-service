package me.soknight.minigram.chats.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.hide
import io.ktor.utils.io.ExperimentalKtorApi
import me.soknight.minigram.chats.client.ProfileClientFactory
import me.soknight.minigram.chats.routing.chatMemberRoutes
import me.soknight.minigram.chats.routing.chatMessageRoutes
import me.soknight.minigram.chats.routing.chatRoutes
import me.soknight.minigram.chats.routing.userKeyRoutes
import me.soknight.minigram.chats.service.ChatMemberService
import me.soknight.minigram.chats.service.ChatMessageService
import me.soknight.minigram.chats.service.ChatService
import me.soknight.minigram.chats.service.UserKeyService

@OptIn(ExperimentalKtorApi::class)
fun Application.configureRouting(
    chatService: ChatService,
    memberService: ChatMemberService,
    messageService: ChatMessageService,
    profileClientFactory: ProfileClientFactory,
    userKeyService: UserKeyService
) {
    routing {
        get("/health") { call.respond(HttpStatusCode.OK, mapOf("status" to "ok")) }.hide()

        chatRoutes(chatService, profileClientFactory)
        chatMemberRoutes(memberService, profileClientFactory)
        chatMessageRoutes(messageService, profileClientFactory)
        userKeyRoutes(userKeyService, profileClientFactory)
    }
}
