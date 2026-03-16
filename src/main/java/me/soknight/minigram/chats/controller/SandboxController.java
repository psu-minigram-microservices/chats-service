package me.soknight.minigram.chats.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.model.dto.WellKnownUserDto;
import me.soknight.minigram.chats.service.SandboxService;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/sandbox")
@AllArgsConstructor
@ConditionalOnProperty(name = "server.sandbox.enabled", havingValue = "true")
@Tag(name = "Sandbox", description = "Special utility endpoints — for testing only")
public class SandboxController extends ApiControllerBase {

    private final @NonNull SandboxService sandboxService;

    @GetMapping("/well-known-users")
    public @NonNull List<WellKnownUserDto> getWellKnownUsers() {
        return sandboxService.getWellKnownUsers();
    }

}
