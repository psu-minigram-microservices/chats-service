package me.soknight.minigram.chats.model.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record ProfilePageDto(
        @JsonProperty("count") int count,
        @JsonProperty("data") @Nullable List<ProfileDto> data
) {

}
