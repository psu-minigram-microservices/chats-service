@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.service

import me.soknight.minigram.chats.dto.KeyBackupDto
import me.soknight.minigram.chats.dto.KeyBackupRequest
import me.soknight.minigram.chats.dto.UserPublicKeyDto
import me.soknight.minigram.chats.exception.PublicKeyNotFoundException
import me.soknight.minigram.chats.exception.ValidationException
import me.soknight.minigram.chats.repository.UserPublicKeyRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.koin.core.annotation.Single

@Single
class UserKeyService(private val keyRepository: UserPublicKeyRepository) {

    suspend fun upsertPublicKey(userId: Uuid, publicKey: String) {
        if (publicKey.isBlank()) throw ValidationException("public_key must not be blank")
        keyRepository.upsertPublicKey(userId, publicKey)
    }

    suspend fun getPublicKey(userId: Uuid): UserPublicKeyDto? =
        keyRepository.findByUserId(userId)?.let { UserPublicKeyDto(it.userId, it.publicKey) }

    suspend fun upsertBackup(userId: Uuid, request: KeyBackupRequest) {
        if (request.salt.isBlank())       throw ValidationException("salt must not be blank")
        if (request.iv.isBlank())         throw ValidationException("iv must not be blank")
        if (request.ciphertext.isBlank()) throw ValidationException("ciphertext must not be blank")
        keyRepository.findByUserId(userId) ?: throw PublicKeyNotFoundException(userId)
        keyRepository.upsertBackup(userId, request.salt, request.iv, request.ciphertext)
    }

    suspend fun getBackup(userId: Uuid): KeyBackupDto? {
        val row = keyRepository.findByUserId(userId) ?: return null
        val salt       = row.backupSalt       ?: return null
        val iv         = row.backupIv         ?: return null
        val ciphertext = row.backupCiphertext ?: return null
        return KeyBackupDto(salt, iv, ciphertext)
    }
}
