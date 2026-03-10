package me.soknight.minigram.chats.service;

import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.attribute.ChatMemberRole;
import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.model.dto.ChatDto;
import me.soknight.minigram.chats.model.entity.ChatEntity;
import me.soknight.minigram.chats.model.entity.ChatMemberEntity;
import me.soknight.minigram.chats.model.request.CreateChatRequest;
import me.soknight.minigram.chats.model.request.EditChatRequest;
import me.soknight.minigram.chats.repository.ChatRepository;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@AllArgsConstructor
public class ChatService {

    private final @NonNull ChatRepository chatRepository;

    @Transactional(readOnly = true)
    public @NonNull Page<ChatDto> getChats(long userId, @NonNull Pageable pageable) {
        return chatRepository.findAllByMemberUserId(userId, pageable).map(ChatDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public @NonNull ChatDto getChat(long userId, long chatId) throws ApiException {
        return ChatDto.fromEntity(getAccessibleChat(chatId, userId));
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
    public @NonNull ChatDto editChat(long userId, long chatId, @NonNull EditChatRequest request) throws ApiException {
        var chat = getAccessibleChat(chatId, userId);

        if (chat.getOwnerId() != userId)
            throw new ApiException(HttpStatus.FORBIDDEN, "access_denied", "Only chat owner can edit the chat");

        if (!chat.isGroup())
            throw new ApiException(HttpStatus.CONFLICT, "chat_edit_not_supported", "Only group chats can be edited");

        if (request.title() != null) {
            var title = normalizeTitle(request.title(), chat.getType());
            chat.updateTitle(title);
        }

        return ChatDto.fromEntity(chat);
    }

    @Transactional
    public @NonNull ChatDto deleteChat(long userId, long chatId) throws ApiException {
        var chat = getAccessibleChat(chatId, userId);

        if (chat.getOwnerId() != userId)
            throw new ApiException(HttpStatus.FORBIDDEN, "access_denied", "Only chat owner can delete the chat");

        var dto = ChatDto.fromEntity(chat);
        chatRepository.delete(chat);
        return dto;
    }

    @NonNull ChatEntity getAccessibleChat(long chatId, long userId) throws ApiException {
        return chatRepository.findAccessibleById(chatId, userId)
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
            @NonNull Set<Long> memberIds
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
            }

            case GROUP -> {
                if (hasBlankTitle(title))
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
