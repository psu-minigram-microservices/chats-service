package me.soknight.minigram.chats.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ErrorDto(
    val errorCode: String,
    val errorMessage: String,
    val payload: JsonElement? = null
)
