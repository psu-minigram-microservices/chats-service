@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.repository

import me.soknight.minigram.chats.domain.UserPublicKeyRow
import me.soknight.minigram.chats.model.UserPublicKeysTable
import me.soknight.minigram.chats.plugin.dbQuery
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.koin.core.annotation.Single

@Single
class UserPublicKeyRepository {

    suspend fun findByUserId(userId: Uuid): UserPublicKeyRow? = dbQuery {
        UserPublicKeysTable.selectAll()
            .where { UserPublicKeysTable.userId eq userId }
            .singleOrNull()?.toRow()
    }

    suspend fun upsertPublicKey(userId: Uuid, publicKey: String): Unit = dbQuery {
        val now = Clock.System.now()
        val exists = UserPublicKeysTable.selectAll()
            .where { UserPublicKeysTable.userId eq userId }
            .any()
        if (exists) {
            UserPublicKeysTable.update({ UserPublicKeysTable.userId eq userId }) {
                it[UserPublicKeysTable.publicKey] = publicKey
                it[UserPublicKeysTable.updatedAt] = now
            }
        } else {
            UserPublicKeysTable.insert {
                it[UserPublicKeysTable.userId]    = userId
                it[UserPublicKeysTable.publicKey] = publicKey
                it[UserPublicKeysTable.createdAt] = now
                it[UserPublicKeysTable.updatedAt] = now
            }
        }
    }

    suspend fun upsertBackup(userId: Uuid, salt: String, iv: String, ciphertext: String): Unit = dbQuery {
        UserPublicKeysTable.update({ UserPublicKeysTable.userId eq userId }) {
            it[UserPublicKeysTable.backupSalt]       = salt
            it[UserPublicKeysTable.backupIv]         = iv
            it[UserPublicKeysTable.backupCiphertext] = ciphertext
            it[UserPublicKeysTable.updatedAt]        = Clock.System.now()
        }
    }

    private fun ResultRow.toRow() = UserPublicKeyRow(
        userId           = this[UserPublicKeysTable.userId],
        publicKey        = this[UserPublicKeysTable.publicKey],
        backupSalt       = this[UserPublicKeysTable.backupSalt],
        backupIv         = this[UserPublicKeysTable.backupIv],
        backupCiphertext = this[UserPublicKeysTable.backupCiphertext],
        createdAt        = this[UserPublicKeysTable.createdAt],
        updatedAt        = this[UserPublicKeysTable.updatedAt]
    )
}
