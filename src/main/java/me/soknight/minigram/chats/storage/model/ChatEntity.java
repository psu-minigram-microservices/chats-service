package me.soknight.minigram.chats.storage.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.soknight.minigram.chats.model.attribute.ChatType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
    private @NonNull ChatType type;

    @Column(name = "title", length = 255)
    private @Nullable String title;

    @Column(name = "owner_id", nullable = false)
    private long ownerId;

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private @NonNull Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private @NonNull Instant updatedAt;

    @OneToMany(mappedBy = "chat", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private final @NonNull List<ChatMemberEntity> members = new ArrayList<>();

    @OneToMany(mappedBy = "chat", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private final @NonNull List<MessageEntity> messages = new ArrayList<>();

    public ChatEntity(@NonNull ChatType type, @Nullable String title, long ownerId) {
        this.type = Objects.requireNonNull(type, "type");
        this.title = title;
        this.ownerId = ownerId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isSaved() {
        return type == ChatType.SAVED;
    }

    public boolean isDirect() {
        return type == ChatType.DIRECT;
    }

    public boolean isGroup() {
        return type == ChatType.GROUP;
    }

}
