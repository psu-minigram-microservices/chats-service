package me.soknight.minigram.chats.config;

import me.soknight.minigram.chats.config.properties.ProfileServiceProperties;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ProfileServiceProperties.class)
public class ProfileServiceConfig {

    @Bean
    public @NonNull RestClient profileServiceRestClient(
            RestClient.@NonNull Builder restClientBuilder,
            @NonNull ProfileServiceProperties properties
    ) {
        return restClientBuilder
                .baseUrl(properties.profile())
                .build();
    }

}