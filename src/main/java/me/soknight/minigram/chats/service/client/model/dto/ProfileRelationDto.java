package me.soknight.minigram.chats.service.client.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.soknight.minigram.chats.service.client.model.attribute.RelationStatus;

public record ProfileRelationDto(
        @JsonProperty("status") RelationStatus status,
        @JsonProperty("profile") ProfileDto profile
) {

}
