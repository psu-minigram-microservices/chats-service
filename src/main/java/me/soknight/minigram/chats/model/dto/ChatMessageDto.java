package me.soknight.minigram.chats.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.soknight.minigram.chats.model.entity.ChatMessageEntity;
import org.jspecify.annotations.NonNull;

import java.time.Instant;

public record ChatMessageDto(
        @JsonProperty("id") long id,
        @JsonProperty("chat") ChatDto chat,
        @JsonProperty("sender") ChatMemberDto sender,
        @JsonProperty("content") String content,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {

    public static @NonNull ChatMessageDto fromEntity(@NonNull ChatMessageEntity message) {
        return new ChatMessageDto(
                message.getMessageId(),
                ChatDto.fromEntity(message.getChat()),
                ChatMemberDto.fromEntity(message.getSender()),
                message.getContent(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }

}
