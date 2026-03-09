package me.soknight.minigram.chats.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.soknight.minigram.chats.storage.model.MessageEntity;

import java.time.Instant;

public record MessageDto(
        @JsonProperty("id") long id,
        @JsonProperty("chat_id") long chatId,
        @JsonProperty("sender_id") long senderId,
        @JsonProperty("content") String content,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {

    public static MessageDto fromEntity(MessageEntity message) {
        return new MessageDto(
                message.getId(),
                message.getChat().getId(),
                message.getSenderId(),
                message.getContent(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }

}
