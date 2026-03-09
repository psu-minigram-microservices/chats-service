package me.soknight.minigram.chats.service;

import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.dto.EditMessageRequest;
import me.soknight.minigram.chats.model.dto.MessageDto;
import me.soknight.minigram.chats.model.dto.SendMessageRequest;
import me.soknight.minigram.chats.storage.model.ChatMemberEntity;
import me.soknight.minigram.chats.storage.model.MessageEntity;
import me.soknight.minigram.chats.storage.repository.ChatMemberRepository;
import me.soknight.minigram.chats.storage.repository.ChatRepository;
import me.soknight.minigram.chats.storage.repository.MessageRepository;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class MessageService {

    private final @NonNull MessageRepository messageRepository;
    private final @NonNull ChatRepository chatRepository;
    private final @NonNull ChatMemberRepository chatMemberRepository;

    @Transactional
    public @NonNull MessageDto sendMessage(
            long senderId,
            long chatId,
            @NonNull SendMessageRequest request
    ) throws ApiException {
        var sender = getMember(chatId, senderId);

        var message = messageRepository.save(new MessageEntity(sender, request.content().trim()));
        chatRepository.updateLastMessageId(chatId, message.getId(), Instant.now());

        return MessageDto.fromEntity(message);
    }

    @Transactional
    public @NonNull MessageDto editMessage(
            long actorUserId,
            long messageId,
            @NonNull EditMessageRequest request
    ) throws ApiException {
        var message = getEditableMessage(messageId, actorUserId);
        message.updateContent(request.content().trim());

        var updatedMessage = messageRepository.save(message);
        return MessageDto.fromEntity(updatedMessage);
    }

    @Transactional
    public void deleteMessage(long actorUserId, long messageId) throws ApiException {
        var message = getEditableMessage(messageId, actorUserId);
        long chatId = message.getChat().getId();

        messageRepository.delete(message);

        var newLastMessageId = messageRepository.findLastIdByChatIdExcluding(chatId, messageId).orElse(null);
        chatRepository.updateLastMessageId(chatId, newLastMessageId, Instant.now());
    }

    private @NonNull ChatMemberEntity getMember(long chatId, long userId) throws ApiException {
        return chatMemberRepository.findById(chatId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "chat_not_found", "Chat {0} not found", chatId));
    }

    private @NonNull MessageEntity getEditableMessage(long messageId, long actorUserId) throws ApiException {
        var message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "message_not_found", "Message {0} not found", messageId));

        if (message.getSenderId() != actorUserId)
            throw new ApiException(HttpStatus.FORBIDDEN, "access_denied", "Only message author can modify the message");

        return message;
    }

}
