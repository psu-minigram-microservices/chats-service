package me.soknight.minigram.chats.config.websocket;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

public final class WebSocketHandshakeHandler extends DefaultHandshakeHandler {

    private static final @NonNull String PRINCIPAL_ATTRIBUTE = "principal";

    @Override
    protected @Nullable Principal determineUser(
            @NonNull ServerHttpRequest request,
            @NonNull WebSocketHandler handler,
            @NonNull Map<String, Object> attributes
    ) {
        return (Principal) attributes.get(PRINCIPAL_ATTRIBUTE);
    }

}
