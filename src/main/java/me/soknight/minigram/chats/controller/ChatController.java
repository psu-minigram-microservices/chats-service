package me.soknight.minigram.chats.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.dto.ChatDto;
import me.soknight.minigram.chats.model.request.CreateChatRequest;
import me.soknight.minigram.chats.model.request.EditChatRequest;
import me.soknight.minigram.chats.service.ChatService;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/chats")
@AllArgsConstructor
@Tag(name = "Chats", description = "Manage chats: create new, edit and delete")
public class ChatController {

    private final @NonNull ChatService chatService;

    // -------------- /chats -------------------------------------------------------------------------------------------

    @GetMapping
    public @NonNull Page<ChatDto> getChats(
            @PageableDefault(sort = {"updatedAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable
    ) throws ApiException {
        return chatService.getChats(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public @NonNull ChatDto createChat(
            @Valid @RequestBody CreateChatRequest request
    ) throws ApiException {
        return chatService.createChat(request);
    }

    // -------------- /chats/{id} --------------------------------------------------------------------------------------

    @GetMapping("/{chat_id}")
    public @NonNull ChatDto getChatById(
            @PathVariable("chat_id") @Positive long chatId
    ) throws ApiException {
        return chatService.getChat(chatId);
    }

    @PatchMapping("/{chat_id}")
    public @NonNull ChatDto editChatById(
            @PathVariable("chat_id") @Positive long chatId,
            @Valid @RequestBody EditChatRequest request
    ) throws ApiException {
        return chatService.editChat(chatId, request);
    }

    @PutMapping("/{chat_id}")
    public @NonNull ChatDto replaceChatById(
            @PathVariable("chat_id") @Positive long chatId,
            @Valid @RequestBody EditChatRequest request
    ) throws ApiException {
        return chatService.editChat(chatId, request);
    }

    @DeleteMapping("/{chat_id}")
    public @NonNull ChatDto deleteChatById(
            @PathVariable("chat_id") @Positive long chatId
    ) throws ApiException {
        return chatService.deleteChat(chatId);
    }

}
