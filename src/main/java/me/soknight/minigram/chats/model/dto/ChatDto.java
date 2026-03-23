package me.soknight.minigram.chats.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.soknight.minigram.chats.model.attribute.ChatType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatDto(
        @JsonProperty("id") long id,
        @JsonProperty("type") ChatType type,
        @JsonProperty("title") String title,
        @JsonProperty("owner_id") UUID ownerId,
        @JsonProperty("members") List<ChatMemberDto> members,
        @JsonProperty("last_message_id") Long lastMessageId,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
}
