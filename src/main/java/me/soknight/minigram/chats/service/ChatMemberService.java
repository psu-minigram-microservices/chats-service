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
import org.jspecify.annotations.Nullable;
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
    public @NonNull Page<ChatMemberDto> getMembers(long chatId, @NonNull Pageable pageable) throws ApiException {
        var actorProfileId = profileClient.resolveMyProfileId();
        getExistingMember(chatId, actorProfileId);
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
    public @NonNull ChatMemberDto getMember(long chatId, @Nullable UUID memberId) throws ApiException {
        var actorProfileId = profileClient.resolveMyProfileId();
        getExistingMember(chatId, actorProfileId);

        var effectiveMemberId = memberId != null ? memberId : actorProfileId;
        return chatDtoMapper.toChatMemberDto(getExistingMember(chatId, effectiveMemberId));
    }

    @Transactional
    public @NonNull ChatMemberDto inviteUser(long chatId, UUID profileId) throws ApiException {
        var actorProfileId = profileClient.resolveMyProfileId();
        var chat = chatService.getAccessibleChat(chatId, actorProfileId);

        if (!chat.isGroup())
            throw new ApiException(HttpStatus.CONFLICT, "chat_invite_not_supported", "Only group chats support invitations");

        if (!chat.getOwnerId().equals(actorProfileId))
            throw new ApiException(HttpStatus.FORBIDDEN, "access_denied", "Only chat owner can invite new members");

        // Validate that the profile exists
        var profile = profileClient.getProfile(profileId);

        if (chatMemberRepository.existsById(chatId, profileId))
            throw new ApiException(HttpStatus.CONFLICT, "member_already_exists", "Profile {0} is already in chat", profileId);

        validateFriendRelation(profileId);

        var member = new ChatMemberEntity(chat, profileId, ChatMemberRole.MEMBER);
        chatMemberRepository.save(member);
        chatRepository.touch(chatId, Instant.now());

        var dto = chatDtoMapper.toChatMemberDto(member);
        eventPublisher.publish(chatId, ChatEvent.memberJoined(chatId, dto));
        return dto;
    }

    @Transactional
    public @NonNull ChatMemberDto leaveChat(long chatId) throws ApiException {
        var actorProfileId = profileClient.resolveMyProfileId();
        var chat = chatService.getAccessibleChat(chatId, actorProfileId);

        if (chat.isSaved())
            throw new ApiException(HttpStatus.CONFLICT, "cannot_leave_chat", "Saved messages chat cannot be left");

        if (chat.getOwnerId().equals(actorProfileId))
            throw new ApiException(HttpStatus.CONFLICT, "owner_cannot_leave_chat", "Chat owner cannot leave their own chat");

        return dropChatMember(actorProfileId, chatId, chat);
    }

    @Transactional
    public @NonNull ChatMemberDto kickUser(long chatId, UUID profileId) throws ApiException {
        var actorProfileId = profileClient.resolveMyProfileId();
        var chat = chatService.getAccessibleChat(chatId, actorProfileId);

        if (!chat.isGroup())
            throw new ApiException(HttpStatus.CONFLICT, "chat_kick_not_supported", "Only group chats support kicking users");

        if (!chat.getOwnerId().equals(actorProfileId))
            throw new ApiException(HttpStatus.FORBIDDEN, "access_denied", "Only chat owner can kick members");

        if (actorProfileId.equals(profileId))
            throw new ApiException(HttpStatus.CONFLICT, "cannot_kick_self", "Use leave chat endpoint to remove yourself");

        return dropChatMember(profileId, chatId, chat);
    }

    private @NonNull ChatMemberDto dropChatMember(UUID profileId, long chatId, @NonNull ChatEntity chat) throws ApiException {
        var member = getExistingMember(chatId, profileId);
        var dto = chatDtoMapper.toChatMemberDto(member);
        chat.getMembers().remove(member);
        chatRepository.touch(chatId, Instant.now());

        eventPublisher.publish(chatId, ChatEvent.memberLeft(chatId, profileId));
        return dto;
    }

    private void validateFriendRelation(UUID profileId) throws ApiException {
        var relation = profileClient.getRelation(profileId, RelationType.OUTGOING);
        var status = relation.status();

        if (status == RelationStatus.FRIEND) return;

        throw new ApiException(
                HttpStatus.FORBIDDEN,
                "relation_not_accepted",
                "Cannot invite profile {0} because relation status is {1}",
                profileId,
                status == null ? "null" : status.getKey()
        );
    }

    private @NonNull ChatMemberEntity getExistingMember(long chatId, UUID profileId) throws ApiException {
        return chatMemberRepository.findById(chatId, profileId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "member_not_found",
                        "Profile {0} is not a member of chat {1}",
                        profileId,
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
