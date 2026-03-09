package me.soknight.minigram.chats.service;

import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.repository.ChatMemberRepository;
import me.soknight.minigram.chats.repository.ChatMessageRepository;
import me.soknight.minigram.chats.repository.ChatRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ChatMessageService {

    private final @NonNull ChatRepository chatRepository;
    private final @NonNull ChatMemberRepository chatMemberRepository;
    private final @NonNull ChatMessageRepository chatMessageRepository;
    private final @NonNull ChatEventPublisher eventPublisher;

//    @Transactional
//    public @NonNull MessageDto sendMessage(
//            long senderId,
//            long chatId,
//            @NonNull SendMessageRequest request
//    ) throws ApiException {
//        var sender = getMember(chatId, senderId);
//
//        var message = messageRepository.save(new MessageEntity(sender, request.content().trim()));
//        chatRepository.updateLastMessageId(chatId, message.getId(), Instant.now());
//
//        var dto = MessageDto.fromEntity(message);
//        eventPublisher.publish(chatId, ChatEvent.messageSent(chatId, dto));
//        return dto;
//    }
//
//    @Transactional
//    public @NonNull MessageDto editMessage(
//            long actorUserId,
//            long messageId,
//            @NonNull EditMessageRequest request
//    ) throws ApiException {
//        var message = getEditableMessage(messageId, actorUserId);
//        message.updateContent(request.content().trim());
//
//        var updatedMessage = messageRepository.save(message);
//        var dto = MessageDto.fromEntity(updatedMessage);
//        eventPublisher.publish(updatedMessage.getChat().getId(), ChatEvent.messageEdited(updatedMessage.getChat().getId(), dto));
//        return dto;
//    }
//
//    @Transactional
//    public void deleteMessage(long actorUserId, long messageId) throws ApiException {
//        var message = getEditableMessage(messageId, actorUserId);
//        long chatId = message.getChat().getId();
//
//        messageRepository.delete(message);
//
//        var newLastMessageId = messageRepository.findLastIdByChatIdExcluding(chatId, messageId).orElse(null);
//        chatRepository.updateLastMessageId(chatId, newLastMessageId, Instant.now());
//
//        eventPublisher.publish(chatId, ChatEvent.messageDeleted(chatId, messageId));
//    }
//
//    private @NonNull ChatMemberEntity getMember(long chatId, long userId) throws ApiException {
//        return chatMemberRepository.findById(chatId, userId)
//                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "chat_not_found", "Chat {0} not found", chatId));
//    }
//
//    private @NonNull MessageEntity getEditableMessage(long messageId, long actorUserId) throws ApiException {
//        var message = messageRepository.findById(messageId)
//                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "message_not_found", "Message {0} not found", messageId));
//
//        if (message.getSenderId() != actorUserId)
//            throw new ApiException(HttpStatus.FORBIDDEN, "access_denied", "Only message author can modify the message");
//
//        return message;
//    }

}
