package me.soknight.minigram.chats.service.client;

import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.service.client.model.attribute.RelationStatus;
import me.soknight.minigram.chats.service.client.model.attribute.RelationType;
import me.soknight.minigram.chats.service.client.model.dto.ProfileDto;
import me.soknight.minigram.chats.service.client.model.dto.ProfilePageDto;
import me.soknight.minigram.chats.service.client.model.dto.ProfileRelationDto;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ProfileClient {

    private final @NonNull RestClient profileServiceRestClient;

    public @NonNull ProfilePageDto getRelations(
            @NonNull RelationStatus status,
            @NonNull RelationType type,
            @Nullable Integer page,
            @Nullable Integer perPage
    ) throws ApiException {
        try {
            var relations = profileServiceRestClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/api/v1/profiles/relations");

                        builder.queryParam("status", status.getKey());
                        builder.queryParam("type", type.getKey());
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

    public @NonNull ProfileRelationDto getRelation(UUID receiverId, @NonNull RelationType type) throws ApiException {
        try {
            var relation = profileServiceRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/profiles/relations/{receiverId}")
                            .queryParam("type", type.getKey())
                            .build(receiverId))
                    .retrieve()
                    .body(ProfileRelationDto.class);

            if (relation == null)
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "profile_service_invalid_response",
                        "Profile service returned empty relation response for {0}",
                        receiverId
                );

            return relation;
        } catch (RestClientException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "profile_service_unavailable",
                    "Failed to fetch relation with {0} from profile service",
                    receiverId
            ).withPayload(ex.getMessage());
        }
    }

    public @NonNull ProfileDto getMyProfile() throws ApiException {
        try {
            var profile = profileServiceRestClient.get()
                    .uri("/api/v1/profiles/me")
                    .retrieve()
                    .body(ProfileDto.class);

            if (profile == null)
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "profile_service_invalid_response",
                        "Profile service returned empty profile response for current user"
                );

            return profile;
        } catch (RestClientException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "profile_service_unavailable",
                    "Failed to fetch current user profile from profile service"
            ).withPayload(ex.getMessage());
        }
    }

    public @NonNull UUID resolveMyProfileId() throws ApiException {
        return getMyProfile().userId();
    }

    public @NonNull UUID resolveProfileId(@NonNull String bearerToken) throws ApiException {
        try {
            var profile = profileServiceRestClient.get()
                    .uri("/api/v1/profiles/me")
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .body(ProfileDto.class);

            if (profile == null)
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "profile_service_invalid_response",
                        "Profile service returned empty profile response for token"
                );

            return profile.userId();
        } catch (RestClientException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "profile_service_unavailable",
                    "Failed to resolve profile ID from token"
            ).withPayload(ex.getMessage());
        }
    }

    public @NonNull ProfileDto getProfile(UUID id) throws ApiException {
        try {
            var profile = profileServiceRestClient.get()
                    .uri("/api/v1/profiles/{id}", id)
                    .retrieve()
                    .body(ProfileDto.class);

            if (profile == null)
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "profile_service_invalid_response",
                        "Profile service returned empty profile response for {0}",
                        id
                );

            return profile;
        } catch (RestClientException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "profile_service_unavailable",
                    "Failed to fetch profile for {0} from profile service",
                    id
            ).withPayload(ex.getMessage());
        }
    }

}
