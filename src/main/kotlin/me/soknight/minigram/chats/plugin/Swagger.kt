package me.soknight.minigram.chats.plugin

import io.ktor.http.*
import io.ktor.openapi.OpenApiDoc
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.openapi.hide
import io.ktor.utils.io.ExperimentalKtorApi

private val apiInfo = OpenApiInfo(
    title = "Chats API",
    version = "1.0",
    description = "Manage chats, members and messages"
)

@OptIn(ExperimentalKtorApi::class)
fun Application.configureSwagger() {
    if (!developmentMode) return

    routing {
        swaggerUI(path = "swagger") {
            info = apiInfo
        }

        get("/openapi") {
            val spec = OpenApiDocSource.Routing(contentType = ContentType.Application.Yaml)
                .read(application, OpenApiDoc(info = apiInfo))!!
            call.respondText(spec.content, spec.contentType)
        }.hide()
    }
}
