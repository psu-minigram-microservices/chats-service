package me.soknight.minigram.chats.storage.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.soknight.minigram.chats.model.attribute.ChatParticipantRole;

import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor
@Entity @Table(name = "chat_participants")
public class ChatParticipantEntity {

    @EmbeddedId
    private ChatParticipantId id;

    @MapsId("chatId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private ChatEntity chat;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private ChatParticipantRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    public ChatParticipantEntity(long userId, ChatParticipantRole role) {
        this.id = new ChatParticipantId(0, userId);
        this.role = Objects.requireNonNull(role, "role");
    }

    public long getUserId() {
        if (id == null) {
            throw new IllegalStateException("Chat participant id is not initialized");
        }
        return id.getUserId();
    }

    void attachTo(ChatEntity chat) {
        if (id == null) {
            throw new IllegalStateException("Chat participant id is not initialized");
        }
        this.chat = Objects.requireNonNull(chat, "chat");
        syncIdWithChat();
    }

    void detach() {
        this.chat = null;
    }

    private void syncIdWithChat() {
        if (chat != null) {
            id.setChatId(chat.getId());
        }
    }

    @PrePersist
    void prePersist() {
        syncIdWithChat();
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }

}
