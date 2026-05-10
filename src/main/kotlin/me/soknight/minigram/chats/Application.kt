package me.soknight.minigram.chats

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import me.soknight.minigram.chats.auth.JwtTokenProvider
import me.soknight.minigram.chats.client.ProfileClient
import me.soknight.minigram.chats.config.loadConfig
import me.soknight.minigram.chats.plugin.*
import me.soknight.minigram.chats.repository.ChatMemberRepository
import me.soknight.minigram.chats.repository.ChatMessageRepository
import me.soknight.minigram.chats.repository.ChatRepository
import me.soknight.minigram.chats.service.*
import me.soknight.minigram.chats.websocket.WebSocketConnectionManager

fun Application.module() {
    val config = loadConfig()

    configureDatabase(config.database)
    configureSerialization()
    configureSecurity(config.jwt)
    configureStatusPages()

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(appJson) }
        expectSuccess = true
    }

    val jwtProvider       = JwtTokenProvider(config.jwt)
    val connectionManager = WebSocketConnectionManager()

    val chatRepo    = ChatRepository()
    val memberRepo  = ChatMemberRepository()
    val messageRepo = ChatMessageRepository()

    val dtoMapper      = ChatDtoMapper(chatRepo, memberRepo)
    val eventPublisher = ChatEventPublisher(connectionManager, memberRepo)

    val chatService    = ChatService(chatRepo, memberRepo, dtoMapper, eventPublisher)
    val memberService  = ChatMemberService(chatRepo, memberRepo, dtoMapper, eventPublisher)
    val messageService = ChatMessageService(chatRepo, memberRepo, messageRepo, dtoMapper, eventPublisher)

    val clientFactory: (String) -> ProfileClient = { token ->
        ProfileClient(httpClient, config.services.profileUrl, token)
    }

    configureRouting(chatService, memberService, messageService, clientFactory)
    configureWebSockets(jwtProvider, connectionManager)
}
