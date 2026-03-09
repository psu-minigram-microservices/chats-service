package me.soknight.minigram.chats.service;

import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.attribute.ChatParticipantRole;
import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.model.dto.ChatDto;
import me.soknight.minigram.chats.model.dto.ChatParticipantDto;
import me.soknight.minigram.chats.model.dto.CreateChatRequest;
import me.soknight.minigram.chats.model.dto.MessageDto;
import me.soknight.minigram.chats.storage.model.ChatEntity;
import me.soknight.minigram.chats.storage.model.ChatParticipantEntity;
import me.soknight.minigram.chats.storage.repository.ChatParticipantRepository;
import me.soknight.minigram.chats.storage.repository.ChatRepository;
import me.soknight.minigram.chats.storage.repository.MessageRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

@Service
@AllArgsConstructor
public class ChatService {

    private static final int DEFAULT_MESSAGES_BATCH_SIZE = 50;

    private final @NonNull ChatRepository chatRepository;
    private final @NonNull ChatParticipantRepository chatParticipantRepository;
    private final @NonNull MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public List<ChatDto> listChats(long userId) {
        return chatRepository.findAllByParticipantUserId(userId).stream()
                .map(ChatDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatDto getChat(long userId, long chatId) throws ApiException {
        return ChatDto.fromEntity(getAccessibleChat(chatId, userId));
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getMessages(long userId, long chatId, int from) throws ApiException {
        if (from < 0) {
            throw new ApiException("invalid_from", "`from` must be greater than or equal to zero");
        }

        ChatEntity chat = getAccessibleChat(chatId, userId);
        return messageRepository.findByChat_IdOrderByCreatedAtDescIdDesc(chat.getId()).stream()
                .skip(from)
                .limit(DEFAULT_MESSAGES_BATCH_SIZE)
                .map(MessageDto::fromEntity)
                .toList();
    }

    @Transactional
    public ChatParticipantDto inviteUser(long actorUserId, long chatId, long invitedUserId) throws ApiException {
        ChatEntity chat = getAccessibleChat(chatId, actorUserId);
        if (!chat.isGroup()) {
            throw new ApiException(HttpStatus.CONFLICT, "chat_invite_not_supported", "Only group chats support invitations");
        }
        if (chat.getOwnerId() != actorUserId) {
            throw new ApiException(HttpStatus.FORBIDDEN, "access_denied", "Only chat owner can invite new participants");
        }
        if (chatParticipantRepository.existsByChat_IdAndUserId(chatId, invitedUserId)) {
            throw new ApiException(HttpStatus.CONFLICT, "participant_already_exists", "User {0} is already in chat", invitedUserId);
        }

        ChatParticipantEntity participant = new ChatParticipantEntity(invitedUserId, ChatParticipantRole.MEMBER);
        chat.addParticipant(participant);
        chat.touch();
        chatRepository.save(chat);

        return ChatParticipantDto.fromEntity(participant);
    }

    @Transactional
    public ChatDto createChat(long ownerId, CreateChatRequest request) throws ApiException {
        LinkedHashSet<Long> participantIds = new LinkedHashSet<>();
        if (request.participantIds() != null) {
            participantIds.addAll(request.participantIds());
        }
        participantIds.remove(ownerId);

        validateChatCreation(request.type(), request.title(), participantIds);

        String title = normalizeTitle(request.title(), request.type());
        ChatEntity chat = new ChatEntity(request.type(), title, ownerId);
        chat.addParticipant(new ChatParticipantEntity(ownerId, ChatParticipantRole.OWNER));

        for (long participantId : participantIds) {
            chat.addParticipant(new ChatParticipantEntity(participantId, ChatParticipantRole.MEMBER));
        }

        ChatEntity savedChat = chatRepository.save(chat);
        return ChatDto.fromEntity(savedChat);
    }

    @Transactional
    public void leaveChat(long userId, long chatId) throws ApiException {
        ChatEntity chat = getAccessibleChat(chatId, userId);
        if (chat.isSavedMessages()) {
            throw new ApiException(HttpStatus.CONFLICT, "cannot_leave_chat", "Saved messages chat cannot be left");
        }
        if (chat.getOwnerId() == userId) {
            throw new ApiException(HttpStatus.CONFLICT, "owner_cannot_leave_chat", "Chat owner cannot leave their own chat");
        }

        ChatParticipantEntity participant = getParticipant(chatId, userId);
        chat.removeParticipant(participant);
        chat.touch();
        chatRepository.save(chat);
    }

    @Transactional
    public void kickUser(long actorUserId, long chatId, long kickedUserId) throws ApiException {
        ChatEntity chat = getAccessibleChat(chatId, actorUserId);
        if (!chat.isGroup()) {
            throw new ApiException(HttpStatus.CONFLICT, "chat_kick_not_supported", "Only group chats support kicking users");
        }
        if (chat.getOwnerId() != actorUserId) {
            throw new ApiException(HttpStatus.FORBIDDEN, "access_denied", "Only chat owner can kick participants");
        }
        if (actorUserId == kickedUserId) {
            throw new ApiException(HttpStatus.CONFLICT, "cannot_kick_self", "Use leave chat endpoint to remove yourself");
        }
        if (chat.getOwnerId() == kickedUserId) {
            throw new ApiException(HttpStatus.CONFLICT, "cannot_kick_owner", "Chat owner cannot be kicked");
        }

        ChatParticipantEntity participant = getParticipant(chatId, kickedUserId);
        chat.removeParticipant(participant);
        chat.touch();
        chatRepository.save(chat);
    }

    @Transactional
    public void deleteChat(long userId, long chatId) throws ApiException {
        ChatEntity chat = getAccessibleChat(chatId, userId);
        if (chat.getOwnerId() != userId) {
            throw new ApiException(HttpStatus.FORBIDDEN, "access_denied", "Only chat owner can delete the chat");
        }

        chatRepository.delete(chat);
    }

    private ChatEntity getAccessibleChat(long chatId, long userId) throws ApiException {
        return chatRepository.findAccessibleById(chatId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "chat_not_found", "Chat {0} not found", chatId));
    }

    private ChatParticipantEntity getParticipant(long chatId, long userId) throws ApiException {
        return chatParticipantRepository.findByChat_IdAndUserId(chatId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "participant_not_found", "User {0} is not a participant of chat {1}", userId, chatId));
    }

    private void validateChatCreation(ChatType type, String title, LinkedHashSet<Long> participantIds) throws ApiException {
        if (type == ChatType.SAVED_MESSAGES && !participantIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_chat_participants", "Saved messages chat cannot contain other participants");
        }
        if (type == ChatType.DIRECT && participantIds.size() != 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_chat_participants", "Direct chat must contain exactly one additional participant");
        }
        if (type == ChatType.GROUP && hasBlankTitle(title)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_chat_title", "Group chat title must not be blank");
        }
    }

    private String normalizeTitle(String rawTitle, ChatType type) {
        if (type != ChatType.GROUP) {
            return null;
        }

        return hasBlankTitle(rawTitle) ? null : rawTitle.trim();
    }

    private boolean hasBlankTitle(String value) {
        return value == null || value.isBlank();
    }

}
