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
            }.describe {
                summary = "Upload public key"
                description = "Uploads or replaces the current user's RSA public key used for end-to-end encryption. The key should be in PEM format."
                responses {
                    HttpStatusCode.OK { description = "Key saved" }
                    HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                }
            }
            get("/{user_id}") {
                val targetId = call.pathUuidParam("user_id")
                val dto      = userKeyService.getPublicKey(targetId)
                    ?: throw PublicKeyNotFoundException(targetId)
                call.respond(dto)
            }.describe {
                summary = "Get user's public key"
                description = "Retrieves the public key for a given user. Use this key to encrypt messages before sending them with `encrypted: true`. Returns 404 if the user has not yet uploaded a public key."
                parameters {
                    path("user_id") { description = "Profile ID of the user whose public key to retrieve" }
                }
                responses {
                    HttpStatusCode.OK { description = "User's public key" }
                    HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                    HttpStatusCode.NotFound { description = "User has not uploaded a public key" }
                }
            }
            route("/backup") {
                put {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    val userId        = profileClient.resolveMyProfileId()
                    val request       = call.receive<KeyBackupRequest>()
                    userKeyService.upsertBackup(userId, request)
                    call.respond(HttpStatusCode.OK)
                }.describe {
                    summary = "Upload key backup"
                    description = "Uploads or replaces the encrypted private key backup. Encrypt the private key client-side using PBKDF2 + AES-GCM before uploading. All values must be Base64-encoded."
                    responses {
                        HttpStatusCode.OK { description = "Backup saved" }
                        HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                    }
                }
                get {
                    val profileClient = profileClientFactory.create(call.bearerToken())
                    val userId        = profileClient.resolveMyProfileId()
                    val dto           = userKeyService.getBackup(userId)
                        ?: throw KeyBackupNotFoundException()
                    call.respond(dto)
                }.describe {
                    summary = "Get key backup"
                    description = "Retrieves the current user's encrypted private key backup. The server never stores the plaintext private key."
                    responses {
                        HttpStatusCode.OK { description = "Encrypted key backup" }
                        HttpStatusCode.Unauthorized { description = "Missing or invalid JWT token" }
                        HttpStatusCode.NotFound { description = "No backup found for this user" }
                    }
                }
            }
        }.describe { tag("E2E Keys") }
    }
}

private fun RoutingCall.pathUuidParam(name: String): Uuid =
    try { Uuid.parse(parameters[name] ?: throw ValidationException("$name is required")) }
    catch (e: IllegalArgumentException) { throw ValidationException("$name must be a valid UUID") }
