package me.soknight.minigram.chats.client

import io.ktor.client.*
import me.soknight.minigram.chats.config.ServicesConfig
import org.koin.core.annotation.Single

@Single
class ProfileClientFactory(
    private val httpClient: HttpClient,
    private val servicesConfig: ServicesConfig
) {
    fun create(token: String): ProfileClient =
        ProfileClient(httpClient, servicesConfig.profileUrl, token)
}
