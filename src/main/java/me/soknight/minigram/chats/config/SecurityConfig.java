package me.soknight.minigram.chats.config;

import lombok.RequiredArgsConstructor;
import me.soknight.minigram.chats.config.properties.JwtProperties;
import me.soknight.minigram.chats.security.JwtAuthenticationFilter;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final @NonNull JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                // firstly, disable some obsolete things
                .cors(AbstractHttpConfigurer::disable)          // CORS will be configured on API gateway or nginx
                .csrf(AbstractHttpConfigurer::disable)          // CSRF isn't required here
                .httpBasic(AbstractHttpConfigurer::disable)     // Basic auth isn't needed
                .formLogin(AbstractHttpConfigurer::disable)     // Form-based login isn't needed

                // we should use stateless session policy on stateless service, ofc
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // configure HTTP endpoints security
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/ws/**",
                                "/docs/openapi/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/webjars/**",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // configure stateless JWT authentication
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

}
