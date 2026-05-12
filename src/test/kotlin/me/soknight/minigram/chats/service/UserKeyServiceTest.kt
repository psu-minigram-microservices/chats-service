@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.service

import io.mockk.*
import kotlinx.coroutines.runBlocking
import me.soknight.minigram.chats.domain.UserPublicKeyRow
import me.soknight.minigram.chats.dto.KeyBackupRequest
import me.soknight.minigram.chats.exception.KeyBackupNotFoundException
import me.soknight.minigram.chats.exception.PublicKeyNotFoundException
import me.soknight.minigram.chats.exception.ValidationException
import me.soknight.minigram.chats.repository.UserPublicKeyRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.test.*

class UserKeyServiceTest {
    private val repo    = mockk<UserPublicKeyRepository>()
    private val service = UserKeyService(repo)
    private val userId  = Uuid.random()

    private fun keyRow(
        backupSalt: String? = null,
        backupIv: String? = null,
        backupCiphertext: String? = null
    ) = UserPublicKeyRow(
        userId, "pubkey-base64",
        backupSalt, backupIv, backupCiphertext,
        Clock.System.now(), Clock.System.now()
    )

    @Test fun `upsertPublicKey blank key throws ValidationException`() = runBlocking {
        assertFailsWith<ValidationException> { service.upsertPublicKey(userId, "  ") }
        coVerify(exactly = 0) { repo.upsertPublicKey(any(), any()) }
    }

    @Test fun `upsertPublicKey valid key calls repository`() = runBlocking {
        coEvery { repo.upsertPublicKey(userId, "pubkey") } just Runs
        service.upsertPublicKey(userId, "pubkey")
        coVerify(exactly = 1) { repo.upsertPublicKey(userId, "pubkey") }
    }

    @Test fun `getPublicKey returns null when not found`() = runBlocking {
        coEvery { repo.findByUserId(userId) } returns null
        assertNull(service.getPublicKey(userId))
    }

    @Test fun `getPublicKey returns dto when found`() = runBlocking {
        coEvery { repo.findByUserId(userId) } returns keyRow()
        val dto = service.getPublicKey(userId)
        assertNotNull(dto)
        assertEquals("pubkey-base64", dto.publicKey)
    }

    @Test fun `upsertBackup blank salt throws ValidationException`() = runBlocking {
        assertFailsWith<ValidationException> {
            service.upsertBackup(userId, KeyBackupRequest("", "iv", "ct"))
        }
    }

    @Test fun `upsertBackup user without key throws PublicKeyNotFoundException`() = runBlocking {
        coEvery { repo.findByUserId(userId) } returns null
        assertFailsWith<PublicKeyNotFoundException> {
            service.upsertBackup(userId, KeyBackupRequest("salt", "iv", "ct"))
        }
    }

    @Test fun `getBackup returns null when no row`() = runBlocking {
        coEvery { repo.findByUserId(userId) } returns null
        assertNull(service.getBackup(userId))
    }

    @Test fun `getBackup returns null when backup fields missing`() = runBlocking {
        coEvery { repo.findByUserId(userId) } returns keyRow()
        assertNull(service.getBackup(userId))
    }

    @Test fun `getBackup returns dto when backup exists`() = runBlocking {
        coEvery { repo.findByUserId(userId) } returns keyRow("salt", "iv", "ct")
        val dto = service.getBackup(userId)
        assertNotNull(dto)
        assertEquals("salt", dto.salt)
    }
}
