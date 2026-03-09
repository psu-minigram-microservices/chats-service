package me.soknight.minigram.chats.storage.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor
@Entity @Table(name = "messages")
public class MessageEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private @NonNull ChatEntity chat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "chat_id", referencedColumnName = "chat_id", insertable = false, updatable = false),
            @JoinColumn(name = "sender_id", referencedColumnName = "user_id")
    })
    private @NonNull ChatMemberEntity sender;

    @Column(name = "content", nullable = false, length = 4000)
    private @NonNull String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private @NonNull Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private @NonNull Instant updatedAt;

    public MessageEntity(@NonNull ChatMemberEntity sender, @NonNull String content) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.chat = sender.getChat();
        this.content = Objects.requireNonNull(content, "content");
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public long getSenderId() {
        return sender.getUserId();
    }

    public void updateContent(@NonNull String content) {
        this.content = Objects.requireNonNull(content, "content");
        this.updatedAt = Instant.now();
    }

}
