package me.soknight.minigram.chats.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.soknight.minigram.chats.model.attribute.ChatMemberRole;

import java.time.Instant;
import java.util.UUID;

public record ChatMemberDto(
        @JsonProperty("profile_id") UUID profileId,
        @JsonProperty("name") String name,
        @JsonProperty("photoUrl") String photoUrl,
        @JsonProperty("role") ChatMemberRole role,
        @JsonProperty("joined_at") Instant joinedAt
) {

}
