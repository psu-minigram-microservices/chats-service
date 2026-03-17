package me.soknight.minigram.chats.service.client;

import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.attribute.RelationStatus;
import me.soknight.minigram.chats.model.dto.client.ProfilePageDto;
import me.soknight.minigram.chats.model.dto.client.ProfileRelationDto;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ProfileRelationsClient {

    private final @NonNull RestClient profileServiceRestClient;

    public @NonNull ProfilePageDto getRelations(
            @Nullable RelationStatus status,
            @Nullable Integer page,
            @Nullable Integer perPage
    ) throws ApiException {
        try {
            var relations = profileServiceRestClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/api/v1/profiles/relations");

                        if (status != null)
                            builder.queryParam("status", status.getKey());
                        if (page != null)
                            builder.queryParam("Page", page);
                        if (perPage != null)
                            builder.queryParam("PerPage", perPage);

                        return builder.build();
                    })
                    .retrieve()
                    .body(ProfilePageDto.class);

            if (relations == null)
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "profile_service_invalid_response",
                        "Profile service returned empty relations response"
                );

            return relations;
        } catch (RestClientException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "profile_service_unavailable",
                    "Failed to fetch relations from profile service"
            ).withPayload(ex.getMessage());
        }
    }

    public @NonNull ProfileRelationDto getRelation(UUID receiverId) throws ApiException {
        try {
            var relation = profileServiceRestClient.get()
                    .uri("/api/v1/profiles/relations/{receiverId}", receiverId)
                    .retrieve()
                    .body(ProfileRelationDto.class);

            if (relation == null)
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "profile_service_invalid_response",
                        "Profile service returned empty relation response for user {0}",
                        receiverId
                );

            return relation;
        } catch (RestClientException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "profile_service_unavailable",
                    "Failed to fetch relation with user {0} from profile service",
                    receiverId
            ).withPayload(ex.getMessage());
        }
    }

    public void setRelation(UUID receiverId, @NonNull RelationStatus status) throws ApiException {
        try {
            profileServiceRestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/profiles/relations/{receiverId}")
                            .queryParam("status", status.getKey())
                            .build(receiverId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "profile_service_unavailable",
                    "Failed to set relation with user {0} in profile service",
                    receiverId
            ).withPayload(ex.getMessage());
        }
    }

    public void deleteRelation(UUID receiverId) throws ApiException {
        try {
            profileServiceRestClient.delete()
                    .uri("/api/v1/profiles/relations/{receiverId}", receiverId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "profile_service_unavailable",
                    "Failed to delete relation with user {0} in profile service",
                    receiverId
            ).withPayload(ex.getMessage());
        }
    }

}