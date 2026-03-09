package me.soknight.minigram.chats.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.soknight.minigram.chats.model.attribute.ChatParticipantRole;
import me.soknight.minigram.chats.storage.model.ChatParticipantEntity;

import java.time.Instant;

public record ChatParticipantDto(
        @JsonProperty("user_id") long userId,
        @JsonProperty("role") ChatParticipantRole role,
        @JsonProperty("joined_at") Instant joinedAt
) {

    public static ChatParticipantDto fromEntity(ChatParticipantEntity participant) {
        return new ChatParticipantDto(
                participant.getUserId(),
                participant.getRole(),
                participant.getJoinedAt()
        );
    }

}
