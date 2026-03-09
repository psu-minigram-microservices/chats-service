package me.soknight.minigram.chats.storage.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.soknight.minigram.chats.model.attribute.ChatType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@NoArgsConstructor
@Entity @Table(name = "chats")
public class ChatEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private ChatType type;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "owner_id", nullable = false)
    private long ownerId;

    @OneToMany(mappedBy = "chat", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatParticipantEntity> participants = new ArrayList<>();

    @OneToMany(mappedBy = "chat", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageEntity> messages = new ArrayList<>();

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ChatEntity(ChatType type, String title, long ownerId) {
        this.type = Objects.requireNonNull(type, "type");
        this.title = title;
        this.ownerId = ownerId;
    }

    public void rename(String title) {
        this.title = title;
    }

    public void addParticipant(ChatParticipantEntity participant) {
        participant.attachTo(this);
        participants.add(participant);
    }

    public void removeParticipant(ChatParticipantEntity participant) {
        if (participant == null) {
            return;
        }

        if (participants.remove(participant)) {
            participant.detach();
        }
    }

    public void addMessage(MessageEntity message) {
        message.attachTo(this);
        messages.add(message);
    }

    public void markLastMessage(MessageEntity message) {
        if (!Objects.equals(message.getChat(), this)) {
            throw new IllegalArgumentException("Message belongs to another chat");
        }
        this.lastMessageId = message.getId();
    }

    public void clearLastMessage() {
        this.lastMessageId = null;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public boolean isSavedMessages() {
        return type == ChatType.SAVED_MESSAGES;
    }

    public boolean isDirect() {
        return type == ChatType.DIRECT;
    }

    public boolean isGroup() {
        return type == ChatType.GROUP;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

}
