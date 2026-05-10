package me.soknight.minigram.chats.di

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import me.soknight.minigram.chats.plugin.appJson
import org.koin.core.annotation.Single

@Single
fun provideHttpClient(): HttpClient = HttpClient(CIO) {
    install(ContentNegotiation) { json(appJson) }
    expectSuccess = true
}
