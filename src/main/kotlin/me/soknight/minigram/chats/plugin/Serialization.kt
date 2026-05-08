package me.soknight.minigram.chats.plugin

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

val appJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults    = true
    explicitNulls     = false
}

fun Application.configureSerialization() {
    install(ContentNegotiation) { json(appJson) }
}
