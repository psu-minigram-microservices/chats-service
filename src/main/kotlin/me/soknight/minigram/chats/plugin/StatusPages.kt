package me.soknight.minigram.chats.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import me.soknight.minigram.chats.dto.ErrorDto
import me.soknight.minigram.chats.exception.AppException
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("StatusPages")

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<AppException> { call, cause ->
            call.respond(cause.statusCode, ErrorDto(cause.errorCode, cause.message ?: cause.errorCode))
        }
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorDto("internal_error", "An unexpected error occurred"))
        }
    }
}
