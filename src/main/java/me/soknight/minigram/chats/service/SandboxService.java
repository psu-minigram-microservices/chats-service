package me.soknight.minigram.chats.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.soknight.minigram.chats.model.dto.WellKnownUserDto;
import me.soknight.minigram.chats.security.JwtTokenProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class SandboxService {

    private static final int WELL_KNOWN_USERS_COUNT = 10;
    private static final @NonNull Duration WELL_KNOWN_USERS_TOKEN_TTL = Duration.ofMinutes(20L);

    private final @NonNull Map<UUID, String> wellKnownUsers = new HashMap<>();
    private final @NonNull ReadWriteLock wellKnownUsersLock = new ReentrantReadWriteLock();

    private final @NonNull JwtTokenProvider jwtTokenProvider;

    public @NonNull List<WellKnownUserDto> getWellKnownUsers() {
        try {
            wellKnownUsersLock.readLock().lock();
            if (!wellKnownUsers.isEmpty()) {
                return wellKnownUsers.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(this::toUserDto)
                        .toList();
            }
        } finally {
            wellKnownUsersLock.readLock().unlock();
        }

        try {
            wellKnownUsersLock.writeLock().lock();

            // double check to prevent desync
            if (wellKnownUsers.isEmpty())
                generateWellKnownUsers();

            return wellKnownUsers.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(this::toUserDto)
                    .toList();
        } finally {
            wellKnownUsersLock.writeLock().unlock();
        }
    }

    private void generateWellKnownUsers() {
        for (int i = 0; i < WELL_KNOWN_USERS_COUNT; i++) {
            var uuid = UUID.randomUUID();
            var token = jwtTokenProvider.generateToken(uuid, WELL_KNOWN_USERS_TOKEN_TTL);
            wellKnownUsers.put(uuid, token);
        }
    }

    private @NonNull WellKnownUserDto toUserDto(Map.@NonNull Entry<UUID, String> entry) {
        return new WellKnownUserDto(entry.getKey(), entry.getValue());
    }

}
