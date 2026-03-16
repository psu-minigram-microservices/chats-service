package me.soknight.minigram.chats.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import me.soknight.minigram.chats.config.properties.JwtProperties;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final @NonNull SecretKey secretKey;
    private final @NonNull JwtParser jwtParser;

    public JwtTokenProvider(@NonNull JwtProperties properties) {
        var keyBytes = Base64.getDecoder().decode(properties.secret());
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.jwtParser = Jwts.parser().verifyWith(secretKey).build();
    }

    public @NonNull String generateToken(@NonNull UUID userId, @NonNull Duration expiration) {
        var now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(secretKey)
                .compact();
    }

    public @NonNull Optional<Claims> parseToken(@NonNull String token) {
        try {
            return Optional.of(jwtParser.parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

}
