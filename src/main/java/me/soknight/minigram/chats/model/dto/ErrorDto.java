package me.soknight.minigram.chats.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record ErrorDto(
        @JsonProperty("error_code") String errorCode,
        @JsonProperty("error_message") String errorMessage,
        @JsonProperty("payload") @JsonInclude(JsonInclude.Include.NON_NULL) Object payload
) {

    public ErrorDto(@NonNull String errorCode, @NonNull String errorMessage) {
        this(errorCode, errorMessage, null);
    }

    public ErrorDto(@NonNull String errorCode, @NonNull String errorMessage, @Nullable Object payload) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.payload = payload;
    }

}