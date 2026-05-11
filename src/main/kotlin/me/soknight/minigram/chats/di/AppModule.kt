package me.soknight.minigram.chats.di

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import me.soknight.minigram.chats.config.*
import me.soknight.minigram.chats.plugin.profileServiceJson
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.module.Module as KoinModule

@Module
@ComponentScan("me.soknight.minigram.chats")
class AppModule(private val application: Application) {
    @Single fun provideAppConfig(): AppConfig = application.loadConfig()
    @Single fun provideJwtConfig(config: AppConfig): JwtConfig = config.jwt
    @Single fun provideDatabaseConfig(config: AppConfig): DatabaseConfig = config.database
    @Single fun provideServicesConfig(config: AppConfig): ServicesConfig = config.services

    @Single
    fun provideHttpClient(): HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(profileServiceJson) }
        expectSuccess = true
    }
}

fun AppModule.buildKoinModule(): KoinModule = module()
