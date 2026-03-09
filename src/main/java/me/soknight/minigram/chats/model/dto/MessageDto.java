package me.soknight.minigram.chats.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.soknight.minigram.chats.model.entity.MessageEntity;
import org.jspecify.annotations.NonNull;

import java.time.Instant;

public record MessageDto(
        @JsonProperty("id") long id,
        @JsonProperty("chat") ChatDto chat,
        @JsonProperty("sender") ChatMemberDto sender,
        @JsonProperty("content") String content,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {

    public static @NonNull MessageDto fromEntity(@NonNull MessageEntity message) {
        return new MessageDto(
                message.getId(),
                ChatDto.fromEntity(message.getChat()),
                ChatMemberDto.fromEntity(message.getSender()),
                message.getContent(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }

}
