package me.soknight.minigram.chats.model.dto.mapper;

import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.dto.ChatDto;
import me.soknight.minigram.chats.model.dto.ChatMemberDto;
import me.soknight.minigram.chats.model.dto.ChatMessageDto;
import me.soknight.minigram.chats.model.entity.ChatEntity;
import me.soknight.minigram.chats.model.entity.ChatMemberEntity;
import me.soknight.minigram.chats.model.entity.ChatMessageEntity;
import me.soknight.minigram.chats.service.client.ProfileClient;
import me.soknight.minigram.chats.service.client.model.dto.ProfileDto;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ChatDtoMapper {

    private final @NonNull ProfileClient profileClient;

    public @NonNull ChatMemberDto toChatMemberDto(@NonNull ChatMemberEntity member) throws ApiException {
        return toChatMemberDto(member, new HashMap<>());
    }

    public @NonNull ChatDto toChatDto(@NonNull ChatEntity chat) throws ApiException {
        try {
            return toChatDto(chat, new HashMap<>());
        } catch (ChatDtoMappingException ex) {
            throw ex.apiException();
        }
    }

    public @NonNull ChatMessageDto toChatMessageDto(@NonNull ChatMessageEntity message) throws ApiException {
        try {
            var profiles = new HashMap<UUID, ProfileDto>();

            return new ChatMessageDto(
                    message.getMessageId(),
                    toChatDto(message.getChat(), profiles),
                    toChatMemberDto(message.getSender(), profiles),
                    message.getContent(),
                    message.getCreatedAt(),
                    message.getUpdatedAt()
            );
        } catch (ChatDtoMappingException ex) {
            throw ex.apiException();
        }
    }

    private @NonNull ChatDto toChatDto(
            @NonNull ChatEntity chat,
            @NonNull Map<UUID, ProfileDto> profiles
    ) throws ApiException {
        var members = chat.getMembers().stream()
                .sorted(Comparator.comparing(ChatMemberEntity::getJoinedAt).thenComparing(ChatMemberEntity::getUserId))
                .map(member -> toUncheckedChatMemberDto(member, profiles))
                .toList();

        return new ChatDto(
                chat.getId(),
                chat.getType(),
                chat.getTitle(),
                chat.getOwnerId(),
                members,
                chat.getLastMessageId(),
                chat.getCreatedAt(),
                chat.getUpdatedAt()
        );
    }

    private @NonNull ChatMemberDto toChatMemberDto(
            @NonNull ChatMemberEntity member,
            @NonNull Map<UUID, ProfileDto> profiles
    ) throws ApiException {
        var profile = getProfile(member.getUserId(), profiles);

        return new ChatMemberDto(
                member.getUserId(),
                profile.name(),
                profile.photoUrl(),
                member.getRole(),
                member.getJoinedAt()
        );
    }

    private @NonNull ChatMemberDto toUncheckedChatMemberDto(
            @NonNull ChatMemberEntity member,
            @NonNull Map<UUID, ProfileDto> profiles
    ) {
        try {
            return toChatMemberDto(member, profiles);
        } catch (ApiException ex) {
            throw new ChatDtoMappingException(ex);
        }
    }

    private @NonNull ProfileDto getProfile(
            UUID userId,
            @NonNull Map<UUID, ProfileDto> profiles
    ) throws ApiException {
        var profile = profiles.get(userId);
        if (profile != null) return profile;

        profile = profileClient.getProfile(userId);
        profiles.put(userId, profile);
        return profile;
    }

    public static final class ChatDtoMappingException extends RuntimeException {

        private final @NonNull ApiException apiException;

        public ChatDtoMappingException(@NonNull ApiException apiException) {
            super(apiException);
            this.apiException = apiException;
        }

        public @NonNull ApiException apiException() {
            return apiException;
        }

    }

}
