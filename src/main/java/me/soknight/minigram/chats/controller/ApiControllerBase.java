package me.soknight.minigram.chats.controller;

import me.soknight.minigram.chats.exception.ApiException;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

public class ApiControllerBase {

    protected long extractUserId(@Nullable Authentication authentication) throws ApiException {
        var name = authentication != null ? authentication.getName() : null;
        if (name == null || name.isBlank())
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "unauthorized",
                    "Missing authenticated user"
            );

        try {
            return Long.parseLong(name);
        } catch (NumberFormatException ex) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "invalid_token_subject",
                    "JWT subject must contain numeric user id"
            );
        }
    }

}
