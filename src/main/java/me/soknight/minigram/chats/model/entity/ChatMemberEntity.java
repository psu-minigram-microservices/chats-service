package me.soknight.minigram.chats.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.soknight.minigram.chats.model.attribute.ChatMemberRole;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity @Table(name = "chat_members")
public class ChatMemberEntity {

    @EmbeddedId
    private @NonNull ChatMemberId id;

    @MapsId("chatId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private @NonNull ChatEntity chat;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private @NonNull ChatMemberRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private @NonNull Instant joinedAt;

    public ChatMemberEntity(@NonNull ChatEntity chat, UUID profileId, @Nullable ChatMemberRole role) {
        this.chat = Objects.requireNonNull(chat, "chat");
        this.id = new ChatMemberId(chat.getId(), profileId);
        this.role = role != null ? role : ChatMemberRole.MEMBER;
        this.joinedAt = Instant.now();
    }

    public long getChatId() {
        return id.chatId();
    }

    public UUID getUserId() {
        return id.profileId();
    }

}
