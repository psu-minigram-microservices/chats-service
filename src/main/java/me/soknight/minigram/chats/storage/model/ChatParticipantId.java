package me.soknight.minigram.chats.storage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Embeddable
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class ChatParticipantId implements Serializable {

    @Column(name = "chat_id", nullable = false)
    private long chatId;

    @Column(name = "user_id", nullable = false)
    private long userId;

    void setChatId(long chatId) {
        this.chatId = chatId;
    }

}
