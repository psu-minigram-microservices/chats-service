@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import me.soknight.minigram.chats.dto.ProfileDto
import me.soknight.minigram.chats.dto.ProfilePageDto
import me.soknight.minigram.chats.dto.ProfileRelationDto
import me.soknight.minigram.chats.exception.ProfileServiceUnavailableException
import me.soknight.minigram.chats.model.RelationStatus
import me.soknight.minigram.chats.model.RelationType
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val logger = LoggerFactory.getLogger(ProfileClient::class.java)

class ProfileClient(
    private val http: HttpClient,
    private val baseUrl: String,
    private val authToken: String
) {
    private fun HttpRequestBuilder.auth() = bearerAuth(authToken)

    suspend fun getMyProfile(): ProfileDto = safe {
        http.get("$baseUrl/api/v1/profiles/me") { auth() }.body()
    }

    suspend fun getProfile(id: Uuid): ProfileDto =
        safeOrNull { http.get("$baseUrl/api/v1/profiles/$id") { auth() }.body() }
            ?: ProfileDto(userId = id, name = id.toString())

    suspend fun resolveMyProfileId(): Uuid = getMyProfile().userId

    suspend fun getRelation(receiverId: Uuid, type: RelationType): ProfileRelationDto? =
        safeOrNull { http.get("$baseUrl/api/v1/profiles/relations/$receiverId") {
            parameter("type", type.name)
            auth()
        }.body() }

    suspend fun getFriends(page: Int = 0, perPage: Int = 200): ProfilePageDto = safe {
        http.get("$baseUrl/api/v1/profiles/relations") {
            parameter("status", RelationStatus.FRIEND.name)
            parameter("type", RelationType.OUTGOING.name)
            parameter("Page", page)
            parameter("PerPage", perPage)
            auth()
        }.body()
    }

    private suspend fun <T> safe(block: suspend () -> T): T = try {
        block()
    } catch (e: ResponseException) {
        logger.error("Profile service HTTP error: {} {}", e.response.status.value, e.response.status.description, e)
        throw ProfileServiceUnavailableException()
    } catch (e: Exception) {
        logger.error("Profile service connection failed: {}", e.message, e)
        throw ProfileServiceUnavailableException()
    }

    private suspend fun <T> safeOrNull(block: suspend () -> T): T? = try {
        block()
    } catch (e: ClientRequestException) {
        if (e.response.status == HttpStatusCode.NotFound) null
        else {
            logger.error("Profile service HTTP error: {} {}", e.response.status.value, e.response.status.description, e)
            throw ProfileServiceUnavailableException()
        }
    } catch (e: ResponseException) {
        logger.error("Profile service HTTP error: {} {}", e.response.status.value, e.response.status.description, e)
        throw ProfileServiceUnavailableException()
    } catch (e: Exception) {
        logger.error("Profile service connection failed: {}", e.message, e)
        throw ProfileServiceUnavailableException()
    }
}
