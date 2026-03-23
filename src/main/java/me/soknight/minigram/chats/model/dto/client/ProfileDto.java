package me.soknight.minigram.chats.model.dto.client;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.UUID;

public record ProfileDto(
        @JsonAlias({"id", "userId", "user_id"}) UUID userId,
        @JsonAlias({"name"}) String name,
        @JsonAlias({"photoUrl", "photo_url"}) String photoUrl
) {

}