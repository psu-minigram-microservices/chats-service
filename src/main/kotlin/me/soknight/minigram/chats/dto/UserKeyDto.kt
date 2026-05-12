@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.dto

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class UserPublicKeyDto(
    val userId: Uuid,
    val publicKey: String
)

@Serializable
data class UpsertPublicKeyRequest(val publicKey: String)

@Serializable
data class KeyBackupRequest(
    val salt: String,
    val iv: String,
    val ciphertext: String
)

@Serializable
data class KeyBackupDto(
    val salt: String,
    val iv: String,
    val ciphertext: String
)
