package me.soknight.minigram.chats.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import me.soknight.minigram.chats.model.attribute.ChatType;

import java.util.List;

public record CreateChatRequest(
        @NotNull ChatType type,
        @Size(max = 255) String title,
        @JsonProperty("member_ids") List<@NotNull @Positive Long> memberIds
) {

}