package me.soknight.minigram.chats.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record ChatMessageDto(
        @JsonProperty("id") long id,
        @JsonProperty("chat") ChatDto chat,
        @JsonProperty("sender") ChatMemberDto sender,
        @JsonProperty("content") String content,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
}
