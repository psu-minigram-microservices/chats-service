package me.soknight.minigram.chats.dto

import kotlinx.serialization.Serializable

@Serializable
data class PageDto<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val total: Int
)
