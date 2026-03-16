package me.soknight.minigram.chats.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record WellKnownUserDto(
        @JsonProperty("user_id") UUID userId,
        @JsonProperty("token") String token
) {

}
