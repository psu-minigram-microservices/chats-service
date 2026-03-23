package me.soknight.minigram.chats.service.client;

import me.soknight.minigram.chats.service.client.model.attribute.RelationStatus;
import me.soknight.minigram.chats.service.client.model.attribute.RelationType;
import me.soknight.minigram.chats.service.client.model.dto.ProfileDto;
import me.soknight.minigram.chats.service.client.model.dto.ProfilePageDto;
import me.soknight.minigram.chats.service.client.model.dto.ProfileRelationDto;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Primary
@Service
public class TestProfileClient extends ProfileClient {

    private final Map<UUID, RelationStatus> statuses = new ConcurrentHashMap<>();

    public TestProfileClient() {
        super(RestClient.builder().baseUrl("http://localhost").build());
    }

    @Override
    public @NonNull ProfilePageDto getRelations(
            @NonNull RelationStatus status,
            @NonNull RelationType type,
            Integer page,
            Integer perPage
    ) {
        List<ProfileDto> data = statuses.entrySet().stream()
                .filter(entry -> entry.getValue() == status)
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> getProfile(entry.getKey()))
                .toList();

        return new ProfilePageDto(data.size(), data);
    }

    @Override
    public @NonNull ProfileRelationDto getRelation(UUID receiverId, @NonNull RelationType type) {
        var status = statuses.getOrDefault(receiverId, RelationStatus.FRIEND);
        return new ProfileRelationDto(status, getProfile(receiverId));
    }

    @Override
    public @NonNull ProfileDto getMyProfile() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var userId = UUID.fromString(auth.getName());
        return getProfile(userId);
    }

    @Override
    public @NonNull ProfileDto getProfile(UUID id) {
        return new ProfileDto(
                id,
                "User " + id.toString().substring(0, 8),
                "https://example.com/" + id + ".png"
        );
    }

    public void setStatus(UUID receiverId, RelationStatus status) {
        statuses.put(receiverId, status);
    }

    public void reset() {
        statuses.clear();
    }

}
