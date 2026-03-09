package me.soknight.minigram.chats.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.storage.model.ChatEntity;
import me.soknight.minigram.chats.storage.model.ChatParticipantEntity;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record ChatDto(
        @JsonProperty("id") long id,
        @JsonProperty("type") ChatType type,
        @JsonProperty("title") String title,
        @JsonProperty("owner_id") long ownerId,
        @JsonProperty("participants") List<ChatParticipantDto> participants,
        @JsonProperty("last_message_id") Long lastMessageId,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {

    public static ChatDto fromEntity(ChatEntity chat) {
        List<ChatParticipantDto> participants = chat.getParticipants().stream()
                .sorted(Comparator.comparing(ChatParticipantEntity::getJoinedAt).thenComparing(ChatParticipantEntity::getUserId))
                .map(ChatParticipantDto::fromEntity)
                .toList();

        return new ChatDto(
                chat.getId(),
                chat.getType(),
                chat.getTitle(),
                chat.getOwnerId(),
                participants,
                chat.getLastMessageId(),
                chat.getCreatedAt(),
                chat.getUpdatedAt()
        );
    }

}
