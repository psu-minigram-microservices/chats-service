package me.soknight.minigram.chats.service;

import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.attribute.ChatMemberRole;
import me.soknight.minigram.chats.model.dto.ChatMemberDto;
import me.soknight.minigram.chats.model.entity.ChatEntity;
import me.soknight.minigram.chats.model.entity.ChatMemberEntity;
import me.soknight.minigram.chats.model.websocket.ChatEvent;
import me.soknight.minigram.chats.repository.ChatMemberRepository;
import me.soknight.minigram.chats.repository.ChatRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@AllArgsConstructor
public class ChatMemberService {

    private final @NonNull ChatRepository chatRepository;
    private final @NonNull ChatMemberRepository chatMemberRepository;
    private final @NonNull ChatService chatService;
    private final @NonNull ChatEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public @NonNull Page<ChatMemberDto> getMembers(long userId, long chatId, @NonNull Pageable pageable) throws ApiException {
        getExistingMember(chatId, userId);
        return chatMemberRepository.findByChatId(chatId, pageable)
                .map(ChatMemberDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public @NonNull ChatMemberDto getMember(long actorUserId, long chatId, long memberId) throws ApiException {
        getExistingMember(chatId, actorUserId);
        return ChatMemberDto.fromEntity(getExistingMember(chatId, memberId));
    }

    @Transactional
    public @NonNull ChatMemberDto inviteUser(long actorUserId, long chatId, long invitedUserId) throws ApiException {
        var chat = chatService.getAccessibleChat(chatId, actorUserId);

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
    public @NonNull ChatMemberDto leaveChat(long userId, long chatId) throws ApiException {
        var chat = chatService.getAccessibleChat(chatId, userId);

        if (chat.isSaved())
            throw new ApiException(HttpStatus.CONFLICT, "cannot_leave_chat", "Saved messages chat cannot be left");

        if (chat.getOwnerId() == userId)
            throw new ApiException(HttpStatus.CONFLICT, "owner_cannot_leave_chat", "Chat owner cannot leave their own chat");

        return dropChatMember(userId, chatId, chat);
    }

    @Transactional
    public @NonNull ChatMemberDto kickUser(long actorUserId, long chatId, long kickedUserId) throws ApiException {
        var chat = chatService.getAccessibleChat(chatId, actorUserId);

        if (!chat.isGroup())
            throw new ApiException(HttpStatus.CONFLICT, "chat_kick_not_supported", "Only group chats support kicking users");

        if (chat.getOwnerId() != actorUserId)
            throw new ApiException(HttpStatus.FORBIDDEN, "access_denied", "Only chat owner can kick members");

        if (actorUserId == kickedUserId)
            throw new ApiException(HttpStatus.CONFLICT, "cannot_kick_self", "Use leave chat endpoint to remove yourself");

        return dropChatMember(kickedUserId, chatId, chat);
    }

    private @NonNull ChatMemberDto dropChatMember(
            long userId,
            long chatId,
            @NonNull ChatEntity chat
    ) throws ApiException {
        var member = getExistingMember(chatId, userId);
        var dto = ChatMemberDto.fromEntity(member);
        chat.getMembers().remove(member);
        chatRepository.touch(chatId, Instant.now());

        eventPublisher.publish(chatId, ChatEvent.memberLeft(chatId, userId));
        return dto;
    }

    private @NonNull ChatMemberEntity getExistingMember(long chatId, long userId) throws ApiException {
        return chatMemberRepository.findById(chatId, userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "member_not_found",
                        "User {0} is not a member of chat {1}",
                        userId,
                        chatId
                ));
    }

}
