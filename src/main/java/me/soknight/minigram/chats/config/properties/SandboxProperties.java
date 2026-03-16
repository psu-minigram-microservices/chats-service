package me.soknight.minigram.chats.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "server.sandbox")
public record SandboxProperties(boolean enabled) {

}
