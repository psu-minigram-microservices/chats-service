@file:OptIn(ExperimentalUuidApi::class, ExperimentalKtorApi::class)

package me.soknight.minigram.chats.routing

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi
import me.soknight.minigram.chats.client.ProfileClientFactory
import me.soknight.minigram.chats.dto.KeyBackupRequest
import me.soknight.minigram.chats.dto.UpsertPublicKeyRequest
import me.soknight.minigram.chats.exception.KeyBackupNotFoundException
import me.soknight.minigram.chats.exception.PublicKeyNotFoundException
import me.soknight.minigram.chats.exception.ValidationException
import me.soknight.minigram.chats.service.UserKeyService
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun Route.userKeyRoutes(userKeyService: UserKeyService, profileClientFactory: ProfileClientFactory) {
    authenticate("jwt") {
        route("/api/v1/keys") {
            put {
                val profileClient = profileClientFactory.create(call.bearerToken())
                val userId        = profileClient.resolveMyProfileId()
                val request       = call.receive<UpsertPublicKeyRequest>()
                userKeyService.upsertPublicKey(userId, request.publicKey)
                call.respond(HttpStatusCode.OK)
            }
            get("/{user_id}") {
                val targetId = call.pathUuidParam("user_id")
                val dto      = userKeyService.getPublicKey(targetId)
                    ?: throw PublicKeyNotFoundException(targetId)
                call.respond(dto)
            }
            route("/backup") {
                put {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    val userId        = profileClient.resolveMyProfileId()
                    val request       = call.receive<KeyBackupRequest>()
                    userKeyService.upsertBackup(userId, request)
                    call.respond(HttpStatusCode.OK)
                }
                get {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    val userId        = profileClient.resolveMyProfileId()
                    val dto           = userKeyService.getBackup(userId)
                        ?: throw KeyBackupNotFoundException()
                    call.respond(dto)
                }
            }
        }.describe { tag("E2E Keys") }
    }
}

private fun RoutingCall.pathUuidParam(name: String): Uuid =
    try { Uuid.parse(parameters[name] ?: throw ValidationException("$name is required")) }
    catch (e: IllegalArgumentException) { throw ValidationException("$name must be a valid UUID") }
