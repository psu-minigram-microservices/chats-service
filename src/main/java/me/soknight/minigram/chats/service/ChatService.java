package me.soknight.minigram.chats.service;

import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.attribute.ChatMemberRole;
import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.model.dto.ChatDto;
import me.soknight.minigram.chats.model.websocket.ChatEvent;
import me.soknight.minigram.chats.model.dto.ChatMemberDto;
import me.soknight.minigram.chats.model.request.CreateChatRequest;
import me.soknight.minigram.chats.model.dto.MessageDto;
import me.soknight.minigram.chats.model.entity.ChatEntity;
import me.soknight.minigram.chats.model.entity.ChatMemberEntity;
import me.soknight.minigram.chats.repository.ChatMemberRepository;
import me.soknight.minigram.chats.repository.ChatRepository;
import me.soknight.minigram.chats.repository.MessageRepository;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@AllArgsConstructor
public class ChatService {

    private final @NonNull ChatRepository chatRepository;
    private final @NonNull ChatMemberRepository chatMemberRepository;
    private final @NonNull MessageRepository messageRepository;
    private final @NonNull ChatEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public @NonNull Page<ChatDto> listChats(long userId, @NonNull Pageable pageable) {
        return chatRepository.findAllByMemberUserId(userId, pageable)
                .map(ChatDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public @NonNull ChatDto getChat(long userId, long chatId) throws ApiException {
        return ChatDto.fromEntity(getAccessibleChat(chatId, userId));
    }

    @Transactional(readOnly = true)
    public @NonNull Page<MessageDto> getMessages(long userId, long chatId, @NonNull Pageable pageable) throws ApiException {
        var chat = getAccessibleChat(chatId, userId);
        return messageRepository.findByChatId(chat.getId(), pageable)
                .map(MessageDto::fromEntity);
    }

    @Transactional
    public @NonNull ChatMemberDto inviteUser(long actorUserId, long chatId, long invitedUserId) throws ApiException {
        var chat = getAccessibleChat(chatId, actorUserId);
        if (!chat.isGroup())
            throw new ApiException(HttpStatus.CONFLICT, "chat_invite_not_supported", "Only group chats support invitations");

        if (chat.getOwnerId() != actorUserId)
            throw new ApiException(HttpStatus.FORBIDDEN, "access_denied", "Only chat owner can invite new members");

        if (chatMemberRepository.existsById(chatId, invitedUserId))
            throw new ApiException(HttpStatus.CONFLICT, "member_already_exists", "User {0} is already in chat", invitedUserId);

        var member = new ChatMemberEntity(chat, invitedUserId, ChatMemberRole.MEMBER);
        chatMemberRepository.save(member);
        chatRepository.touch(chatId, Instant.now());

        var dto = ChatMemberDto.fromEntity(member);
        eventPublisher.publish(chatId, ChatEvent.memberJoined(chatId, dto));
        return dto;
    }

    @Transactional
    public @NonNull ChatDto createChat(long ownerId, @NonNull CreateChatRequest request) throws ApiException {
        Set<Long> memberIds = new LinkedHashSet<>();
        if (request.memberIds() != null)
            memberIds.addAll(request.memberIds());

        memberIds.remove(ownerId);

        validateChatCreation(request.type(), request.title(), memberIds);

        var title = normalizeTitle(request.title(), request.type());
        var chat = new ChatEntity(request.type(), title, ownerId);

        chat.getMembers().add(new ChatMemberEntity(chat, ownerId, ChatMemberRole.OWNER));

        for (long memberId : memberIds)
            chat.getMembers().add(new ChatMemberEntity(chat, memberId, ChatMemberRole.MEMBER));

        return ChatDto.fromEntity(chatRepository.save(chat));
    }

    @Transactional
    public void leaveChat(long userId, long chatId) throws ApiException {
        var chat = getAccessibleChat(chatId, userId);
        if (chat.isSaved())
            throw new ApiException(HttpStatus.CONFLICT, "cannot_leave_chat", "Saved messages chat cannot be left");

        if (chat.getOwnerId() == userId)
            throw new ApiException(HttpStatus.CONFLICT, "owner_cannot_leave_chat", "Chat owner cannot leave their own chat");

        var member = getMember(chatId, userId);
        chat.getMembers().remove(member);
        chatRepository.touch(chatId, Instant.now());

        eventPublisher.publish(chatId, ChatEvent.memberLeft(chatId, userId));
    }

    @Transactional
    public void kickUser(long actorUserId, long chatId, long kickedUserId) throws ApiException {
        var chat = getAccessibleChat(chatId, actorUserId);
        if (!chat.isGroup())
            throw new ApiException(HttpStatus.CONFLICT, "chat_kick_not_supported", "Only group chats support kicking users");

        if (chat.getOwnerId() != actorUserId)
            throw new ApiException(HttpStatus.FORBIDDEN, "access_denied", "Only chat owner can kick members");

        if (actorUserId == kickedUserId)
            throw new ApiException(HttpStatus.CONFLICT, "cannot_kick_self", "Use leave chat endpoint to remove yourself");

        var member = getMember(chatId, kickedUserId);
        chat.getMembers().remove(member);
        chatRepository.touch(chatId, Instant.now());

        eventPublisher.publish(chatId, ChatEvent.memberLeft(chatId, kickedUserId));
    }

    @Transactional
    public void deleteChat(long userId, long chatId) throws ApiException {
        var chat = getAccessibleChat(chatId, userId);
        if (chat.getOwnerId() != userId)
            throw new ApiException(HttpStatus.FORBIDDEN, "access_denied", "Only chat owner can delete the chat");

        chatRepository.delete(chat);
    }

    private @NonNull ChatEntity getAccessibleChat(long chatId, long userId) throws ApiException {
        return chatRepository.findAccessibleById(chatId, userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "chat_not_found",
                        "Chat {0} not found",
                        chatId
                ));
    }

    private @NonNull ChatMemberEntity getMember(long chatId, long userId) throws ApiException {
        return chatMemberRepository.findById(chatId, userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "member_not_found",
                        "User {0} is not a member of chat {1}",
                        userId,
                        chatId
                ));
    }

    private void validateChatCreation(
            @NonNull ChatType type,
            @Nullable String title,
            @NonNull Set<Long> memberIds
    ) throws ApiException {
        switch (type) {
            case SAVED -> {
                if (memberIds.isEmpty()) return;

                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_chat_members",
                        "Saved messages chat cannot contain other members"
                );
            }

            case DIRECT -> {
                if (memberIds.size() == 1) return;

                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_chat_members",
                        "Direct chat must contain exactly one additional member"
                );
            }

            case GROUP ->  {
                if (!hasBlankTitle(title)) return;

                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_chat_title",
                        "Group chat title must not be blank"
                );
            }
        }
    }

    private @Nullable String normalizeTitle(@Nullable String rawTitle, @NonNull ChatType type) {
        if (type != ChatType.GROUP) return null;

        return hasBlankTitle(rawTitle) ? null : rawTitle.trim();
    }

    private boolean hasBlankTitle(@Nullable String value) {
        return value == null || value.isBlank();
    }

}
