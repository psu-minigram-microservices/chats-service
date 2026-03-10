package me.soknight.minigram.chats.controller;

import me.soknight.minigram.chats.exception.ApiException;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public class ApiControllerBase {

    protected UUID extractUserId(@Nullable Authentication authentication) throws ApiException {
        var name = authentication != null ? authentication.getName() : null;
        if (name == null || name.isBlank())
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "unauthorized",
                    "Missing authenticated user"
            );

        try {
            return UUID.fromString(name);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "invalid_token_subject",
                    "JWT subject must contain a valid UUID user id"
            );
        }
    }

}
