package me.soknight.minigram.chats.service;

import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.dto.EditMessageRequest;
import me.soknight.minigram.chats.model.dto.MessageDto;
import me.soknight.minigram.chats.model.dto.SendMessageRequest;
import me.soknight.minigram.chats.storage.model.ChatEntity;
import me.soknight.minigram.chats.storage.model.MessageEntity;
import me.soknight.minigram.chats.storage.repository.ChatParticipantRepository;
import me.soknight.minigram.chats.storage.repository.ChatRepository;
import me.soknight.minigram.chats.storage.repository.MessageRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class MessageService {

    private final @NonNull MessageRepository messageRepository;
    private final @NonNull ChatRepository chatRepository;
    private final @NonNull ChatParticipantRepository chatParticipantRepository;
    
    @Transactional
    public MessageDto sendMessage(long senderId, long chatId, SendMessageRequest request) throws ApiException {
        ChatEntity chat = getAccessibleChat(chatId, senderId);

        MessageEntity message = new MessageEntity(senderId, request.content().trim());
        chat.addMessage(message);
        messageRepository.save(message);
        chat.markLastMessage(message);
        chat.touch();
        chatRepository.save(chat);

        return MessageDto.fromEntity(message);
    }

    @Transactional
    public MessageDto editMessage(long actorUserId, long messageId, EditMessageRequest request) throws ApiException {
        MessageEntity message = getEditableMessage(messageId, actorUserId);
        message.editContent(request.content().trim());
        MessageEntity updatedMessage = messageRepository.save(message);
        return MessageDto.fromEntity(updatedMessage);
    }

    @Transactional
    public void deleteMessage(long actorUserId, long messageId) throws ApiException {
        MessageEntity message = getEditableMessage(messageId, actorUserId);
        ChatEntity chat = message.getChat();

        if (chat.getLastMessageId() != null && message.getId() == chat.getLastMessageId()) {
            messageRepository.findTopByChat_IdAndIdNotOrderByCreatedAtDescIdDesc(chat.getId(), message.getId())
                    .ifPresentOrElse(chat::markLastMessage, chat::clearLastMessage);
            chat.touch();
            chatRepository.save(chat);
        }

        messageRepository.delete(message);
    }

    private ChatEntity getAccessibleChat(long chatId, long userId) throws ApiException {
        ChatEntity chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "chat_not_found", "Chat {0} not found", chatId));

        if (!chatParticipantRepository.existsByChat_IdAndUserId(chatId, userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "chat_not_found", "Chat {0} not found", chatId);
        }

        return chat;
    }

    private MessageEntity getEditableMessage(long messageId, long actorUserId) throws ApiException {
        MessageEntity message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "message_not_found", "Message {0} not found", messageId));

        if (message.getSenderId() != actorUserId) {
            throw new ApiException(HttpStatus.FORBIDDEN, "access_denied", "Only message author can modify the message");
        }

        return message;
    }

}
