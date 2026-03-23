package me.soknight.minigram.chats.service.client;

import me.soknight.minigram.chats.model.attribute.RelationStatus;
import me.soknight.minigram.chats.model.attribute.RelationType;
import me.soknight.minigram.chats.model.dto.client.ProfileDto;
import me.soknight.minigram.chats.model.dto.client.ProfileRelationDto;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Primary
@Service
public class TestProfileRelationsClient extends ProfileRelationsClient {

    private final Map<UUID, RelationStatus> statuses = new ConcurrentHashMap<>();

    public TestProfileRelationsClient() {
        super(RestClient.builder().baseUrl("http://localhost").build());
    }

    @Override
    public @NonNull ProfileRelationDto getRelation(UUID receiverId, @NonNull RelationType type) {
        var status = statuses.getOrDefault(receiverId, RelationStatus.FRIEND);
        return new ProfileRelationDto(status, new ProfileDto(receiverId, "Test User", null));
    }

    public void setStatus(UUID receiverId, RelationStatus status) {
        statuses.put(receiverId, status);
    }

    public void reset() {
        statuses.clear();
    }

}
