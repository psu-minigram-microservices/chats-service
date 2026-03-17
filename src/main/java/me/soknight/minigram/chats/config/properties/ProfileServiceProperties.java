package me.soknight.minigram.chats.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "server.services")
public record ProfileServiceProperties(
        @NotBlank String profile
) {

}