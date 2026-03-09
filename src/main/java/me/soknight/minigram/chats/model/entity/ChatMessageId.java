package me.soknight.minigram.chats.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public record ChatMessageId(
        @Column(name = "chat_id", nullable = false) long chatId,
        @Column(name = "message_id", nullable = false) long messageId
) implements Serializable {

}
