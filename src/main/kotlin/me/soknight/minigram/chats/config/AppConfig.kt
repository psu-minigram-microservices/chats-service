package me.soknight.minigram.chats.config

import io.ktor.server.application.*

data class AppConfig(
    val jwt: JwtConfig,
    val services: ServicesConfig,
    val database: DatabaseConfig
)

data class DatabaseConfig(
    val url: String,
    val driver: String,
    val username: String,
    val password: String,
    val maxPoolSize: Int,
    val minIdle: Int,
    val migrationLocations: List<String> = listOf("classpath:db/migration"),
)

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
)

data class ServicesConfig(
    val profileUrl: String,
)

fun Application.loadConfig(): AppConfig {
    val config = environment.config
    return AppConfig(
        jwt = JwtConfig(
            secret   = config.property("chats.jwt.secret").getString(),
            issuer   = config.property("chats.jwt.issuer").getString(),
            audience = config.property("chats.jwt.audience").getString()
        ),
        services = ServicesConfig(
            profileUrl = config.property("chats.services.profileUrl").getString()
        ),
        database = DatabaseConfig(
            url        = config.property("chats.database.url").getString(),
            driver     = config.property("chats.database.driver").getString(),
            username   = config.property("chats.database.username").getString(),
            password   = config.property("chats.database.password").getString(),
            maxPoolSize = config.property("chats.database.maxPoolSize").getString().toInt(),
            minIdle    = config.property("chats.database.minIdle").getString().toInt(),
            migrationLocations = config.propertyOrNull("chats.database.migrationLocations")
                ?.getList() ?: listOf("classpath:db/migration")
        )
    )
}
