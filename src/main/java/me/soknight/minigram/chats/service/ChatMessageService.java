package me.soknight.minigram.chats.service;

import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.attribute.RelationStatus;
import me.soknight.minigram.chats.model.attribute.RelationType;
import me.soknight.minigram.chats.model.dto.ChatMessageDto;
import me.soknight.minigram.chats.model.entity.ChatEntity;
import me.soknight.minigram.chats.model.entity.ChatMemberEntity;
import me.soknight.minigram.chats.model.entity.ChatMessageEntity;
import me.soknight.minigram.chats.model.entity.ChatMessageId;
import me.soknight.minigram.chats.model.request.EditMessageRequest;
import me.soknight.minigram.chats.model.request.SendMessageRequest;
import me.soknight.minigram.chats.model.websocket.ChatEvent;
import me.soknight.minigram.chats.repository.ChatMemberRepository;
import me.soknight.minigram.chats.repository.ChatMessageRepository;
import me.soknight.minigram.chats.repository.ChatRepository;
import me.soknight.minigram.chats.service.client.ProfileRelationsClient;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ChatMessageService {

    private final @NonNull ChatRepository chatRepository;
    private final @NonNull ChatMemberRepository chatMemberRepository;
    private final @NonNull ChatMessageRepository chatMessageRepository;
    private final @NonNull ProfileRelationsClient profileRelationsClient;
    private final @NonNull ChatEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public @NonNull Page<ChatMessageDto> getMessages(
            UUID userId,
            long chatId,
            @NonNull Pageable pageable
    ) throws ApiException {
        getMember(chatId, userId);
        return chatMessageRepository.findByChatId(chatId, pageable).map(ChatMessageDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public @NonNull ChatMessageDto getMessage(UUID userId, long chatId, long messageId) throws ApiException {
        getMember(chatId, userId);

        var message = chatMessageRepository.findById(chatId, messageId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "message_not_found",
                "Message {0} not found",
                messageId
        ));

        return ChatMessageDto.fromEntity(message);
    }

    @Transactional
    public @NonNull ChatMessageDto sendMessage(
            UUID senderId,
            long chatId,
            @NonNull SendMessageRequest request
    ) throws ApiException {
        var sender = getMember(chatId, senderId);
        validateMessageSending(senderId, sender.getChat());

        chatRepository.incrementMessageSequence(chatId);
        long messageId = chatRepository.getMessageSequence(chatId);

        var id = new ChatMessageId(chatId, messageId);
        var message = chatMessageRepository.save(new ChatMessageEntity(id, sender, request.content().trim()));
        chatRepository.updateLastMessageId(chatId, message.getMessageId(), Instant.now());

        var dto = ChatMessageDto.fromEntity(message);
        eventPublisher.publish(chatId, ChatEvent.messageSent(chatId, dto));
        return dto;
    }

    @Transactional
    public @NonNull ChatMessageDto editMessage(
            UUID actorUserId,
            long chatId,
            long messageId,
            @NonNull EditMessageRequest request
    ) throws ApiException {
        var message = getEditableMessage(chatId, messageId, actorUserId);
        message.updateContent(request.content().trim());

        var updatedMessage = chatMessageRepository.save(message);
        var dto = ChatMessageDto.fromEntity(updatedMessage);
        eventPublisher.publish(chatId, ChatEvent.messageEdited(chatId, dto));
        return dto;
    }

    @Transactional
    public @NonNull ChatMessageDto deleteMessage(UUID actorUserId, long chatId, long messageId) throws ApiException {
        var message = getEditableMessage(chatId, messageId, actorUserId);

        var dto = ChatMessageDto.fromEntity(message);
        chatMessageRepository.delete(message);

        var newLastMessageId = chatMessageRepository.findLastMessageIdByChatIdExcluding(chatId, messageId).orElse(null);
        chatRepository.updateLastMessageId(chatId, newLastMessageId, Instant.now());

        eventPublisher.publish(chatId, ChatEvent.messageDeleted(chatId, messageId));
        return dto;
    }

    private void validateMessageSending(UUID senderId, @NonNull ChatEntity chat) throws ApiException {
        if (!chat.isDirect()) return;

        var receiverId = chat.getMembers().stream()
                .map(ChatMemberEntity::getUserId)
                .filter(userId -> !userId.equals(senderId))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "invalid_chat_members",
                        "Direct chat must contain exactly one additional member"
                ));

        var relation = profileRelationsClient.getRelation(receiverId, RelationType.OUTGOING);
        var status = relation.status();

        if (status == RelationStatus.FRIEND) return;

        throw new ApiException(
                HttpStatus.FORBIDDEN,
                "relation_not_accepted",
                "Cannot send messages to user {0} because relation status is {1}",
                receiverId,
                status == null ? "null" : status.getKey()
        );
    }

    private @NonNull ChatMemberEntity getMember(long chatId, UUID userId) throws ApiException {
        return chatMemberRepository.findById(chatId, userId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "chat_not_found",
                "Chat {0} not found",
                chatId
        ));
    }

    private @NonNull ChatMessageEntity getEditableMessage(long chatId, long messageId, UUID actorUserId) throws ApiException {
        getMember(chatId, actorUserId);

        var message = chatMessageRepository.findById(chatId, messageId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "message_not_found",
                "Message {0} not found",
                messageId
        ));

        if (!message.getSenderId().equals(actorUserId))
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "access_denied",
                    "Only message author can modify the message"
            );

        return message;
    }

}
