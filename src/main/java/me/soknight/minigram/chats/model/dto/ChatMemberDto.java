package me.soknight.minigram.chats.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.soknight.minigram.chats.model.attribute.ChatMemberRole;
import me.soknight.minigram.chats.storage.model.ChatMemberEntity;
import org.jspecify.annotations.NonNull;

import java.time.Instant;

public record ChatMemberDto(
        @JsonProperty("user_id") long userId,
        @JsonProperty("role") ChatMemberRole role,
        @JsonProperty("joined_at") Instant joinedAt
) {

    public static @NonNull ChatMemberDto fromEntity(@NonNull ChatMemberEntity member) {
        return new ChatMemberDto(member.getUserId(), member.getRole(), member.getJoinedAt());
    }

}
