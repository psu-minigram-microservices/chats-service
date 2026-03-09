package me.soknight.minigram.chats.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "server.jwt")
public record JwtProperties(String secret) {

}
