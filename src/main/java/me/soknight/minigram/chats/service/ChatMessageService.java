package me.soknight.minigram.chats.service;

import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.dto.ChatMessageDto;
import me.soknight.minigram.chats.model.dto.mapper.ChatDtoMapper;
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
import me.soknight.minigram.chats.service.client.ProfileClient;
import me.soknight.minigram.chats.service.client.model.attribute.RelationStatus;
import me.soknight.minigram.chats.service.client.model.attribute.RelationType;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    private final @NonNull ProfileClient profileClient;
    private final @NonNull ChatEventPublisher eventPublisher;
    private final @NonNull ChatDtoMapper chatDtoMapper;

    @Transactional(readOnly = true)
    public @NonNull Page<ChatMessageDto> getMessages(long chatId, @NonNull Pageable pageable) throws ApiException {
        var actorProfileId = profileClient.resolveMyProfileId();
        getMember(chatId, actorProfileId);
        var page = chatMessageRepository.findByChatId(chatId, pageable);

        try {
            var messages = page.getContent().stream()
                    .map(this::toUncheckedChatMessageDto)
                    .toList();

            return new PageImpl<>(messages, pageable, page.getTotalElements());
        } catch (ChatDtoMapper.ChatDtoMappingException ex) {
            throw ex.apiException();
        }
    }

    @Transactional(readOnly = true)
    public @NonNull ChatMessageDto getMessage(long chatId, long messageId) throws ApiException {
        var actorProfileId = profileClient.resolveMyProfileId();
        getMember(chatId, actorProfileId);

        var message = chatMessageRepository.findById(chatId, messageId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "message_not_found",
                "Message {0} not found",
                messageId
        ));

        return chatDtoMapper.toChatMessageDto(message);
    }

    @Transactional
    public @NonNull ChatMessageDto sendMessage(long chatId, @NonNull SendMessageRequest request) throws ApiException {
        var senderProfileId = profileClient.resolveMyProfileId();
        var sender = getMember(chatId, senderProfileId);
        validateMessageSending(senderProfileId, sender.getChat());

        chatRepository.incrementMessageSequence(chatId);
        long messageId = chatRepository.getMessageSequence(chatId);

        var id = new ChatMessageId(chatId, messageId);
        var message = chatMessageRepository.save(new ChatMessageEntity(id, sender, request.content().trim()));
        chatRepository.updateLastMessageId(chatId, message.getMessageId(), Instant.now());

        var dto = chatDtoMapper.toChatMessageDto(message);
        eventPublisher.publish(chatId, ChatEvent.messageSent(chatId, dto));
        return dto;
    }

    @Transactional
    public @NonNull ChatMessageDto editMessage(long chatId, long messageId, @NonNull EditMessageRequest request) throws ApiException {
        var actorProfileId = profileClient.resolveMyProfileId();
        var message = getEditableMessage(chatId, messageId, actorProfileId);
        message.updateContent(request.content().trim());

        var updatedMessage = chatMessageRepository.save(message);
        var dto = chatDtoMapper.toChatMessageDto(updatedMessage);
        eventPublisher.publish(chatId, ChatEvent.messageEdited(chatId, dto));
        return dto;
    }

    @Transactional
    public @NonNull ChatMessageDto deleteMessage(long chatId, long messageId) throws ApiException {
        var actorProfileId = profileClient.resolveMyProfileId();
        var message = getEditableMessage(chatId, messageId, actorProfileId);

        var dto = chatDtoMapper.toChatMessageDto(message);
        chatMessageRepository.delete(message);

        var newLastMessageId = chatMessageRepository.findLastMessageIdByChatIdExcluding(chatId, messageId).orElse(null);
        chatRepository.updateLastMessageId(chatId, newLastMessageId, Instant.now());

        eventPublisher.publish(chatId, ChatEvent.messageDeleted(chatId, messageId));
        return dto;
    }

    private void validateMessageSending(UUID senderProfileId, @NonNull ChatEntity chat) throws ApiException {
        if (!chat.isDirect()) return;

        var receiverId = chat.getMembers().stream()
                .map(ChatMemberEntity::getUserId)
                .filter(userId -> !userId.equals(senderProfileId))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "invalid_chat_members",
                        "Direct chat must contain exactly one additional member"
                ));

        var relation = profileClient.getRelation(receiverId, RelationType.OUTGOING);
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

    private @NonNull ChatMemberEntity getMember(long chatId, UUID profileId) throws ApiException {
        return chatMemberRepository.findById(chatId, profileId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "chat_not_found",
                "Chat {0} not found",
                chatId
        ));
    }

    private @NonNull ChatMessageEntity getEditableMessage(long chatId, long messageId, UUID actorProfileId) throws ApiException {
        getMember(chatId, actorProfileId);

        var message = chatMessageRepository.findById(chatId, messageId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "message_not_found",
                "Message {0} not found",
                messageId
        ));

        if (!message.getSenderId().equals(actorProfileId))
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "access_denied",
                    "Only message author can modify the message"
            );

        return message;
    }

    private @NonNull ChatMessageDto toUncheckedChatMessageDto(@NonNull ChatMessageEntity message) {
        try {
            return chatDtoMapper.toChatMessageDto(message);
        } catch (ApiException ex) {
            throw new ChatDtoMapper.ChatDtoMappingException(ex);
        }
    }

}
