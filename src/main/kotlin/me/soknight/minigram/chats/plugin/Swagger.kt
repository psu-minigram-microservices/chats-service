package me.soknight.minigram.chats.plugin

import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*

fun Application.configureSwagger() {
    if (!developmentMode) return

    routing {
        swaggerUI(path = "swagger")
    }
}
