package me.soknight.minigram.chats.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.val;
import me.soknight.minigram.chats.config.properties.JwtProperties;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final @NonNull SecretKey secretKey;
    private final @NonNull JwtParser jwtParser;

    private final @NonNull String issuer;
    private final @NonNull String audience;
    private final int expirationTimeMinutes;

    public JwtTokenProvider(@NonNull JwtProperties properties) {
        var keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.jwtParser = Jwts.parser().verifyWith(secretKey).build();

        this.issuer = properties.issuer();
        this.audience = properties.audience();
        this.expirationTimeMinutes = properties.expiration();
    }

    public @NonNull String generateToken(@NonNull UUID userId) {
        var now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(expirationTimeMinutes))))
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

    public boolean validateClaims(@NonNull Claims claims) {
        var now = new Date();

        var expiration = claims.getExpiration();
        if (expiration == null || expiration.before(now))
            return false;

        if (!issuer.equals(claims.getIssuer()))
            return false;

        var audience = claims.getAudience();
        if (audience == null || !audience.contains(this.audience))
            return false;

        val subject = claims.getSubject();
        return subject != null && !subject.isBlank();
    }

}
