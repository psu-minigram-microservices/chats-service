package me.soknight.minigram.chats.model.request;

import jakarta.validation.constraints.Size;

public record EditChatRequest(
        @Size(max = 255) String title
) {

}