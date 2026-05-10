package me.soknight.minigram.chats

import io.ktor.client.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import me.soknight.minigram.chats.auth.JwtTokenProvider
import me.soknight.minigram.chats.client.ProfileClient
import me.soknight.minigram.chats.config.loadConfig
import me.soknight.minigram.chats.plugin.*
import me.soknight.minigram.chats.repository.ChatMemberRepository
import me.soknight.minigram.chats.repository.ChatMessageRepository
import me.soknight.minigram.chats.repository.ChatRepository
import me.soknight.minigram.chats.service.*
import me.soknight.minigram.chats.websocket.WebSocketConnectionManager

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module(clientFactory: ((String) -> ProfileClient)? = null) {
    val config = loadConfig()

    configureDatabase(config.database)
    configureSerialization()
    configureSecurity(config.jwt)
    configureStatusPages()

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

    val resolvedClientFactory: (String) -> ProfileClient = clientFactory ?: run {
        val httpClient = HttpClient(CIO) {
            install(ContentNegotiation) { json(appJson) }
            expectSuccess = true
        }
        val factory: (String) -> ProfileClient = { token ->
            ProfileClient(httpClient, config.services.profileUrl, token)
        }
        factory
    }

    configureRouting(chatService, memberService, messageService, resolvedClientFactory)
    configureWebSockets(jwtProvider, connectionManager)
}
