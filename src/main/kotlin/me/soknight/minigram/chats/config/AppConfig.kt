package me.soknight.minigram.chats.config

import io.ktor.server.application.*

data class AppConfig(
    val jwt: JwtConfig,
    val services: ServicesConfig,
    val database: DatabaseConfig
)

data class JwtConfig(val secret: String, val issuer: String, val audience: String)
data class ServicesConfig(val profileUrl: String)
data class DatabaseConfig(
    val url: String,
    val driver: String,
    val username: String,
    val password: String,
    val maxPoolSize: Int,
    val minIdle: Int
)

fun Application.loadConfig(): AppConfig {
    val c = environment.config
    return AppConfig(
        jwt = JwtConfig(
            secret   = c.property("chats.jwt.secret").getString(),
            issuer   = c.property("chats.jwt.issuer").getString(),
            audience = c.property("chats.jwt.audience").getString()
        ),
        services = ServicesConfig(
            profileUrl = c.property("chats.services.profileUrl").getString()
        ),
        database = DatabaseConfig(
            url        = c.property("chats.database.url").getString(),
            driver     = c.property("chats.database.driver").getString(),
            username   = c.property("chats.database.username").getString(),
            password   = c.property("chats.database.password").getString(),
            maxPoolSize = c.property("chats.database.maxPoolSize").getString().toInt(),
            minIdle    = c.property("chats.database.minIdle").getString().toInt()
        )
    )
}
