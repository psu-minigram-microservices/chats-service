package me.soknight.minigram.chats.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.dto.ChatDto;
import me.soknight.minigram.chats.model.request.CreateChatRequest;
import me.soknight.minigram.chats.model.request.EditChatRequest;
import me.soknight.minigram.chats.service.ChatService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/chats")
@AllArgsConstructor
public class ChatController extends ApiControllerBase {

    private final @NonNull ChatService chatService;

    // -------------- /chats -------------------------------------------------------------------------------------------

    @GetMapping
    public @NonNull Page<ChatDto> getChats(
            @PageableDefault(sort = {"updatedAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatService.getChats(extractUserId(authentication), pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public @NonNull ChatDto createChat(
            @Valid @RequestBody CreateChatRequest request,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatService.createChat(extractUserId(authentication), request);
    }

    // -------------- /chats/{id} --------------------------------------------------------------------------------------

    @GetMapping("/{chat_id}")
    public @NonNull ChatDto getChatById(
            @PathVariable("chat_id") @Positive long chatId,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatService.getChat(extractUserId(authentication), chatId);
    }

    @PatchMapping("/{chat_id}")
    public @NonNull ChatDto editChatById(
            @PathVariable("chat_id") @Positive long chatId,
            @Valid @RequestBody EditChatRequest request,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatService.editChat(extractUserId(authentication), chatId, request);
    }

    @PutMapping("/{chat_id}")
    public @NonNull ChatDto replaceChatById(
            @PathVariable("chat_id") @Positive long chatId,
            @Valid @RequestBody EditChatRequest request,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatService.editChat(extractUserId(authentication), chatId, request);
    }

    @DeleteMapping("/{chat_id}")
    public @NonNull ChatDto deleteChatById(
            @PathVariable("chat_id") @Positive long chatId,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatService.deleteChat(extractUserId(authentication), chatId);
    }

}
