package me.soknight.minigram.chats.model.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.soknight.minigram.chats.model.attribute.RelationStatus;

public record ProfileRelationDto(
        @JsonProperty("status") RelationStatus status,
        @JsonProperty("profile") ProfileDto profile
) {

}