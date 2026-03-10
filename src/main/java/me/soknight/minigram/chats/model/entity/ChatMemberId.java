package me.soknight.minigram.chats.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record ChatMemberId(
        @Column(name = "chat_id", nullable = false) long chatId,
        @Column(name = "user_id", nullable = false) UUID userId
) implements Serializable {

}
