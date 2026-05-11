@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import me.soknight.minigram.chats.client.ProfileClient
import me.soknight.minigram.chats.client.ProfileClientFactory
import me.soknight.minigram.chats.dto.ProfileDto
import me.soknight.minigram.chats.dto.ProfilePageDto
import me.soknight.minigram.chats.dto.ProfileRelationDto
import me.soknight.minigram.chats.model.RelationStatus
import java.util.Date
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

const val TEST_SECRET   = "test-secret-at-least-32-characters-long"
const val TEST_ISSUER   = "TestIssuer"
const val TEST_AUDIENCE = "TestAudience"

fun testToken(userId: Uuid): String = JWT.create()
    .withSubject(userId.toString())
    .withIssuer(TEST_ISSUER)
    .withAudience(TEST_AUDIENCE)
    .withExpiresAt(Date(System.currentTimeMillis() + 3_600_000))
    .sign(Algorithm.HMAC256(TEST_SECRET))

fun mockProfileClient(
    selfId: Uuid = Uuid.random(),
    friendStatus: RelationStatus = RelationStatus.FRIEND
): ProfileClient = mockk {
    coEvery { getMyProfile() } returns ProfileDto(selfId, "Test User")
    coEvery { getProfile(any()) } answers {
        val id = firstArg<Uuid>()
        ProfileDto(id, "User ${id.toString().take(4)}")
    }
    coEvery { resolveMyProfileId() } returns selfId
    coEvery { getRelation(any(), any()) } returns ProfileRelationDto(friendStatus, ProfileDto(selfId, "User"))
    coEvery { getFriends(any(), any()) } returns ProfilePageDto(0, emptyList())
}

fun mockProfileClientFactory(
    selfId: Uuid = Uuid.random(),
    friendStatus: RelationStatus = RelationStatus.FRIEND
): ProfileClientFactory = mockk {
    every { create(any()) } returns mockProfileClient(selfId, friendStatus)
}
