@file:OptIn(ExperimentalSerializationApi::class)

package me.soknight.minigram.chats.plugin

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

val appJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults    = true
    explicitNulls     = false
    namingStrategy    = JsonNamingStrategy.SnakeCase
}

val profileServiceJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults    = true
    explicitNulls     = false
}

fun Application.configureSerialization() {
    install(ContentNegotiation) { json(appJson) }
}
