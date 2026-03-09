package me.soknight.minigram.chats.config.websocket;

import lombok.RequiredArgsConstructor;
import me.soknight.minigram.chats.security.JwtTokenProvider;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final @NonNull String TOKEN_PARAMETER = "token";
    private static final @NonNull String PRINCIPAL_ATTRIBUTE = "principal";

    private final @NonNull JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler handler,
            @NonNull Map<String, Object> attributes
    ) {
        var token = extractToken(request);
        if (token == null) return false;

        var claims = jwtTokenProvider.parseToken(token).orElse(null);
        if (claims == null) return false;

        var userId = claims.getSubject();
        if (userId == null || userId.isBlank()) return false;

        attributes.put(PRINCIPAL_ATTRIBUTE, (Principal) () -> userId);
        return true;
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler handler,
            @Nullable Exception exception
    ) {
        // no-op
    }

    private @Nullable String extractToken(@NonNull ServerHttpRequest request) {
        var params = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        return params.getFirst(TOKEN_PARAMETER);
    }

}
