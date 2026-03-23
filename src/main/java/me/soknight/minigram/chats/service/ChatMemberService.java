package me.soknight.minigram.chats.service;

import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.attribute.ChatMemberRole;
import me.soknight.minigram.chats.model.dto.ChatMemberDto;
import me.soknight.minigram.chats.model.dto.mapper.ChatDtoMapper;
import me.soknight.minigram.chats.model.entity.ChatEntity;
import me.soknight.minigram.chats.model.entity.ChatMemberEntity;
import me.soknight.minigram.chats.model.websocket.ChatEvent;
import me.soknight.minigram.chats.repository.ChatMemberRepository;
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
public class ChatMemberService {

    private final @NonNull ChatRepository chatRepository;
    private final @NonNull ChatMemberRepository chatMemberRepository;
    private final @NonNull ChatService chatService;
    private final @NonNull ProfileClient profileClient;
    private final @NonNull ChatEventPublisher eventPublisher;
    private final @NonNull ChatDtoMapper chatDtoMapper;

    @Transactional(readOnly = true)
    public @NonNull Page<ChatMemberDto> getMembers(UUID userId, long chatId, @NonNull Pageable pageable) throws ApiException {
        getExistingMember(chatId, userId);
        var page = chatMemberRepository.findByChatId(chatId, pageable);

        try {
            var members = page.getContent().stream()
                    .map(this::toUncheckedChatMemberDto)
                    .toList();

            return new PageImpl<>(members, pageable, page.getTotalElements());
        } catch (ChatDtoMapper.ChatDtoMappingException ex) {
            throw ex.apiException();
        }
    }

    @Transactional(readOnly = true)
    public @NonNull ChatMemberDto getMember(UUID actorUserId, long chatId, UUID memberId) throws ApiException {
        getExistingMember(chatId, actorUserId);
        return chatDtoMapper.toChatMemberDto(getExistingMember(chatId, memberId));
    }

    @Transactional
    public @NonNull ChatMemberDto inviteUser(UUID actorUserId, long chatId, UUID invitedUserId) throws ApiException {
        var chat = chatService.getAccessibleChat(chatId, actorUserId);

        if (!chat.isGroup())
            throw new ApiException(HttpStatus.CONFLICT, "chat_invite_not_supported", "Only group chats support invitations");

        if (!chat.getOwnerId().equals(actorUserId))
            throw new ApiException(HttpStatus.FORBIDDEN, "access_denied", "Only chat owner can invite new members");

        if (chatMemberRepository.existsById(chatId, invitedUserId))
            throw new ApiException(HttpStatus.CONFLICT, "member_already_exists", "User {0} is already in chat", invitedUserId);

        validateFriendRelation(invitedUserId);

        var member = new ChatMemberEntity(chat, invitedUserId, ChatMemberRole.MEMBER);
        chatMemberRepository.save(member);
        chatRepository.touch(chatId, Instant.now());

        var dto = chatDtoMapper.toChatMemberDto(member);
        eventPublisher.publish(chatId, ChatEvent.memberJoined(chatId, dto));
        return dto;
    }

    @Transactional
    public @NonNull ChatMemberDto leaveChat(UUID userId, long chatId) throws ApiException {
        var chat = chatService.getAccessibleChat(chatId, userId);

        if (chat.isSaved())
            throw new ApiException(HttpStatus.CONFLICT, "cannot_leave_chat", "Saved messages chat cannot be left");

        if (chat.getOwnerId().equals(userId))
            throw new ApiException(HttpStatus.CONFLICT, "owner_cannot_leave_chat", "Chat owner cannot leave their own chat");

        return dropChatMember(userId, chatId, chat);
    }

    @Transactional
    public @NonNull ChatMemberDto kickUser(UUID actorUserId, long chatId, UUID kickedUserId) throws ApiException {
        var chat = chatService.getAccessibleChat(chatId, actorUserId);

        if (!chat.isGroup())
            throw new ApiException(HttpStatus.CONFLICT, "chat_kick_not_supported", "Only group chats support kicking users");

        if (!chat.getOwnerId().equals(actorUserId))
            throw new ApiException(HttpStatus.FORBIDDEN, "access_denied", "Only chat owner can kick members");

        if (actorUserId.equals(kickedUserId))
            throw new ApiException(HttpStatus.CONFLICT, "cannot_kick_self", "Use leave chat endpoint to remove yourself");

        return dropChatMember(kickedUserId, chatId, chat);
    }

    private @NonNull ChatMemberDto dropChatMember(
            UUID userId,
            long chatId,
            @NonNull ChatEntity chat
    ) throws ApiException {
        var member = getExistingMember(chatId, userId);
        var dto = chatDtoMapper.toChatMemberDto(member);
        chat.getMembers().remove(member);
        chatRepository.touch(chatId, Instant.now());

        eventPublisher.publish(chatId, ChatEvent.memberLeft(chatId, userId));
        return dto;
    }

    private void validateFriendRelation(UUID invitedUserId) throws ApiException {
        var relation = profileClient.getRelation(invitedUserId, RelationType.OUTGOING);
        var status = relation.status();

        if (status == RelationStatus.FRIEND) return;

        throw new ApiException(
                HttpStatus.FORBIDDEN,
                "relation_not_accepted",
                "Cannot invite user {0} because relation status is {1}",
                invitedUserId,
                status == null ? "null" : status.getKey()
        );
    }

    private @NonNull ChatMemberEntity getExistingMember(long chatId, UUID userId) throws ApiException {
        return chatMemberRepository.findById(chatId, userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "member_not_found",
                        "User {0} is not a member of chat {1}",
                        userId,
                        chatId
                ));
    }

    private @NonNull ChatMemberDto toUncheckedChatMemberDto(@NonNull ChatMemberEntity member) {
        try {
            return chatDtoMapper.toChatMemberDto(member);
        } catch (ApiException ex) {
            throw new ChatDtoMapper.ChatDtoMappingException(ex);
        }
    }

}
