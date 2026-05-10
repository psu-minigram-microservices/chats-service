package me.soknight.minigram.chats.di

import io.ktor.server.application.*
import me.soknight.minigram.chats.config.*
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("me.soknight.minigram.chats")
class AppModule(private val application: Application) {
    @Single fun provideAppConfig(): AppConfig = application.loadConfig()
    @Single fun provideJwtConfig(config: AppConfig): JwtConfig = config.jwt
    @Single fun provideDatabaseConfig(config: AppConfig): DatabaseConfig = config.database
    @Single fun provideServicesConfig(config: AppConfig): ServicesConfig = config.services
}
