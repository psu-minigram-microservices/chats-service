package me.soknight.minigram.chats.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import me.soknight.minigram.chats.config.properties.JwtProperties;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class JwtTokenProvider {

    private final @NonNull JwtParser jwtParser;

    public JwtTokenProvider(@NonNull JwtProperties properties) {
        var keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        var key = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.jwtParser = Jwts.parser().verifyWith(key).build();
    }

    public @NonNull Optional<Claims> parseToken(@NonNull String token) {
        try {
            return Optional.of(jwtParser.parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

}
