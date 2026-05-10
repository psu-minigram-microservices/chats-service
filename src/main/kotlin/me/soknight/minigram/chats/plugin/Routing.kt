package me.soknight.minigram.chats.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.soknight.minigram.chats.client.ProfileClient
import me.soknight.minigram.chats.routing.chatMemberRoutes
import me.soknight.minigram.chats.routing.chatMessageRoutes
import me.soknight.minigram.chats.routing.chatRoutes
import me.soknight.minigram.chats.service.ChatMemberService
import me.soknight.minigram.chats.service.ChatMessageService
import me.soknight.minigram.chats.service.ChatService

fun Application.configureRouting(
    chatService: ChatService,
    memberService: ChatMemberService,
    messageService: ChatMessageService,
    clientFactory: (String) -> ProfileClient
) {
    routing {
        get("/health") { call.respond(HttpStatusCode.OK, mapOf("status" to "ok")) }

        chatRoutes(chatService, clientFactory)
        chatMemberRoutes(memberService, clientFactory)
        chatMessageRoutes(messageService, clientFactory)
    }
}
