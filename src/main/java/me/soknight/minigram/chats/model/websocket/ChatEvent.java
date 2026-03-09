package me.soknight.minigram.chats.model.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.soknight.minigram.chats.model.dto.ChatMemberDto;
import me.soknight.minigram.chats.model.dto.ChatMessageDto;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record ChatEvent(
        @JsonProperty("type") @NonNull Type type,
        @JsonProperty("chat_id") long chatId,
        @JsonProperty("payload") @Nullable Object payload
) {

    public enum Type {
        MESSAGE_SENT,
        MESSAGE_EDITED,
        MESSAGE_DELETED,
        MEMBER_JOINED,
        MEMBER_LEFT
    }

    public static @NonNull ChatEvent messageSent(long chatId, @NonNull ChatMessageDto message) {
        return new ChatEvent(Type.MESSAGE_SENT, chatId, message);
    }

    public static @NonNull ChatEvent messageEdited(long chatId, @NonNull ChatMessageDto message) {
        return new ChatEvent(Type.MESSAGE_EDITED, chatId, message);
    }

    public static @NonNull ChatEvent messageDeleted(long chatId, long messageId) {
        return new ChatEvent(Type.MESSAGE_DELETED, chatId, messageId);
    }

    public static @NonNull ChatEvent memberJoined(long chatId, @NonNull ChatMemberDto member) {
        return new ChatEvent(Type.MEMBER_JOINED, chatId, member);
    }

    public static @NonNull ChatEvent memberLeft(long chatId, long userId) {
        return new ChatEvent(Type.MEMBER_LEFT, chatId, userId);
    }

}
