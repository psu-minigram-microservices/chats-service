package me.soknight.minigram.chats.storage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public record ChatMemberId(
        @Column(name = "chat_id", nullable = false) long chatId,
        @Column(name = "user_id", nullable = false) long userId
) implements Serializable {

}
