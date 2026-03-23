package me.soknight.minigram.chats.service;

import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.attribute.ChatMemberRole;
import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.model.dto.ChatDto;
import me.soknight.minigram.chats.model.dto.ChatMemberDto;
import me.soknight.minigram.chats.model.dto.mapper.ChatDtoMapper;
import me.soknight.minigram.chats.model.entity.ChatEntity;
import me.soknight.minigram.chats.model.entity.ChatMemberEntity;
import me.soknight.minigram.chats.model.request.CreateChatRequest;
import me.soknight.minigram.chats.model.request.EditChatRequest;
import me.soknight.minigram.chats.model.websocket.ChatEvent;
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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ChatService {

    private final @NonNull ChatRepository chatRepository;
    private final @NonNull ProfileClient profileClient;
    private final @NonNull ChatEventPublisher eventPublisher;
    private final @NonNull ChatDtoMapper chatDtoMapper;

    @Transactional(readOnly = true)
    public @NonNull Page<ChatDto> getChats(@NonNull Pageable pageable) throws ApiException {
        var actorProfileId = profileClient.resolveMyProfileId();
        var page = chatRepository.findAllByMemberUserId(actorProfileId, pageable);

        try {
            var chats = page.getContent().stream()
                    .map(this::toUncheckedChatDto)
                    .toList();

            return new PageImpl<>(chats, pageable, page.getTotalElements());
        } catch (ChatDtoMapper.ChatDtoMappingException ex) {
            throw ex.apiException();
        }
    }

    @Transactional(readOnly = true)
    public @NonNull ChatDto getChat(long chatId) throws ApiException {
        var actorProfileId = profileClient.resolveMyProfileId();
        return chatDtoMapper.toChatDto(getAccessibleChat(chatId, actorProfileId));
    }

    @Transactional
    public @NonNull ChatDto createChat(@NonNull CreateChatRequest request) throws ApiException {
        var ownerProfileId = profileClient.resolveMyProfileId();
        Set<UUID> memberIds = new LinkedHashSet<>();
        if (request.memberIds() != null)
            memberIds.addAll(request.memberIds());

        memberIds.remove(ownerProfileId);

        validateChatCreation(request.type(), request.title(), memberIds);

        var title = normalizeTitle(request.title(), request.type());
        var chat = new ChatEntity(request.type(), title, ownerProfileId);

        chat.getMembers().add(new ChatMemberEntity(chat, ownerProfileId, ChatMemberRole.OWNER));

        for (UUID memberId : memberIds)
            chat.getMembers().add(new ChatMemberEntity(chat, memberId, ChatMemberRole.MEMBER));

        var dto = chatDtoMapper.toChatDto(chatRepository.save(chat));
        eventPublisher.publish(dto.id(), ChatEvent.chatCreated(dto));
        return dto;
    }

    @Transactional
    public @NonNull ChatDto editChat(long chatId, @NonNull EditChatRequest request) throws ApiException {
        var actorProfileId = profileClient.resolveMyProfileId();
        var chat = getAccessibleChat(chatId, actorProfileId);

        if (!chat.getOwnerId().equals(actorProfileId))
            throw new ApiException(HttpStatus.FORBIDDEN, "access_denied", "Only chat owner can edit the chat");

        if (!chat.isGroup())
            throw new ApiException(HttpStatus.CONFLICT, "chat_edit_not_supported", "Only group chats can be edited");

        if (request.title() != null) {
            var title = normalizeTitle(request.title(), chat.getType());
            chat.updateTitle(title);
        }

        var dto = chatDtoMapper.toChatDto(chat);
        eventPublisher.publish(dto.id(), ChatEvent.chatUpdated(dto));
        return dto;
    }

    @Transactional
    public @NonNull ChatDto deleteChat(long chatId) throws ApiException {
        var actorProfileId = profileClient.resolveMyProfileId();
        var chat = getAccessibleChat(chatId, actorProfileId);

        if (!chat.getOwnerId().equals(actorProfileId))
            throw new ApiException(HttpStatus.FORBIDDEN, "access_denied", "Only chat owner can delete the chat");

        var dto = chatDtoMapper.toChatDto(chat);
        var memberProfileIds = dto.members().stream()
                .map(ChatMemberDto::profileId)
                .toList();

        chatRepository.delete(chat);
        eventPublisher.publishToUsers(memberProfileIds, ChatEvent.chatDeleted(dto));
        return dto;
    }

    @NonNull ChatEntity getAccessibleChat(long chatId, UUID profileId) throws ApiException {
        return chatRepository.findAccessibleById(chatId, profileId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "chat_not_found",
                        "Chat {0} not found",
                        chatId
                ));
    }

    private void validateChatCreation(
            @NonNull ChatType type,
            @Nullable String title,
            @NonNull Set<UUID> memberIds
    ) throws ApiException {
        switch (type) {
            case SAVED -> {
                if (!memberIds.isEmpty())
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "invalid_chat_members",
                            "Saved messages chat cannot contain other members"
                    );
            }

            case DIRECT -> {
                if (memberIds.size() != 1)
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "invalid_chat_members",
                            "Direct chat must contain exactly one additional member"
                    );

                validateFriendRelations(memberIds, "Direct chat can be created only with friends");
            }

            case GROUP -> {
                if (hasBlankTitle(title))
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "invalid_chat_title",
                            "Group chat title must not be blank"
                    );

                validateFriendRelations(memberIds, "Only friends can be added to a group chat");
            }
        }
    }

    private void validateFriendRelations(@NonNull Set<UUID> memberIds, @NonNull String message) throws ApiException {
        for (UUID memberId : memberIds) {
            var relation = profileClient.getRelation(memberId, RelationType.OUTGOING);
            var status = relation.status();

            if (status == RelationStatus.FRIEND) continue;

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "relation_not_accepted",
                    "{0} Relation status with user {1} is {2}",
                    message,
                    memberId,
                    status == null ? "null" : status.getKey()
            );
        }
    }

    private @Nullable String normalizeTitle(@Nullable String rawTitle, @NonNull ChatType type) {
        if (type != ChatType.GROUP) return null;
        return hasBlankTitle(rawTitle) ? null : rawTitle.trim();
    }

    private boolean hasBlankTitle(@Nullable String value) {
        return value == null || value.isBlank();
    }

    private @NonNull ChatDto toUncheckedChatDto(@NonNull ChatEntity chat) {
        try {
            return chatDtoMapper.toChatDto(chat);
        } catch (ApiException ex) {
            throw new ChatDtoMapper.ChatDtoMappingException(ex);
        }
    }

}
