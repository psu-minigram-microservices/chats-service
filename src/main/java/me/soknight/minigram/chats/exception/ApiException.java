package me.soknight.minigram.chats.exception;

import me.soknight.minigram.chats.model.dto.ErrorDto;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.text.MessageFormat;

public class ApiException extends GenericErrorException {

    private @Nullable Object payload;

    public ApiException(
            @NonNull String errorCode,
            @NonNull String errorMessage,
            @Nullable Object... args
    ) {
        this(HttpStatus.BAD_REQUEST, errorCode, errorMessage, args);
    }

    public ApiException(
            @NonNull HttpStatusCode statusCode,
            @NonNull String errorCode,
            @NonNull String errorMessage,
            @Nullable Object... args
    ) {
        super(statusCode, errorCode, formatMessage(errorMessage, args));
    }

    public @NonNull ApiException withPayload(@Nullable Object payload) {
        this.payload = payload;
        return this;
    }

    private static @Nullable String formatMessage(
            @Nullable String errorMessage,
            @Nullable Object... args
    ) {
        if (errorMessage == null || errorMessage.isEmpty()) return errorMessage;
        if (args == null || args.length == 0) return errorMessage;
        return MessageFormat.format(errorMessage, args);
    }

    @Override
    public @NonNull ErrorDto constructModel() {
        return new ErrorDto(errorCode, errorMessage, payload);
    }

}