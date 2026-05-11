@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.dto

import kotlinx.serialization.Serializable
import me.soknight.minigram.chats.model.RelationStatus
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class ProfileDto(
    val userId: Uuid,
    val name: String,
    val photoUrl: String? = null
)

@Serializable
data class ProfilePageDto(val count: Int, val data: List<ProfileDto>)

@Serializable
data class ProfileRelationDto(val status: RelationStatus, val profile: ProfileDto)
