package me.soknight.minigram.chats.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.model.entity.ChatEntity;
import me.soknight.minigram.chats.model.entity.ChatMemberEntity;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.util.Comparator;
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

    public static @NonNull ChatDto fromEntity(@NonNull ChatEntity chat) {
        var members = chat.getMembers().stream()
                .sorted(Comparator.comparing(ChatMemberEntity::getJoinedAt).thenComparing(ChatMemberEntity::getUserId))
                .map(ChatMemberDto::fromEntity)
                .toList();

        return new ChatDto(
                chat.getId(),
                chat.getType(),
                chat.getTitle(),
                chat.getOwnerId(),
                members,
                chat.getLastMessageId(),
                chat.getCreatedAt(),
                chat.getUpdatedAt()
        );
    }

}
