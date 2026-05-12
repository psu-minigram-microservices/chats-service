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
    description = """
        API for managing chats, members, messages and end-to-end encryption keys.

        All endpoints require a JWT Bearer token in the `Authorization` header.
        Tokens are issued by the auth-service.
    """.trimIndent()
)

@OptIn(ExperimentalKtorApi::class)
fun Application.configureSwagger() {
    if (!developmentMode) return

    routing {
        swaggerUI(path = "swagger") {
            info = apiInfo
            source = OpenApiDocSource.Routing(contentType = ContentType.Application.Json)
        }

        get("/openapi") {
            val spec = OpenApiDocSource.Routing(contentType = ContentType.Application.Json)
                .read(application, OpenApiDoc(info = apiInfo))!!
            call.respondText(spec.content, spec.contentType)
        }.hide()
    }
}
