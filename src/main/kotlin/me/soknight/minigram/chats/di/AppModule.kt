package me.soknight.minigram.chats.di

import io.ktor.server.application.*
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("me.soknight.minigram.chats")
class AppModule {
    @Single
    fun provideApplication(): Application = error("Application should be provided by koin-ktor plugin")
}
