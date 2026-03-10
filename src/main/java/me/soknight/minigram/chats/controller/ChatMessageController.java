package me.soknight.minigram.chats.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.dto.ChatMessageDto;
import me.soknight.minigram.chats.model.request.EditMessageRequest;
import me.soknight.minigram.chats.model.request.SendMessageRequest;
import me.soknight.minigram.chats.service.ChatMessageService;
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
public class ChatMessageController extends ApiControllerBase {

    private final @NonNull ChatMessageService chatMessageService;

    // -------------- /chats/{id}/messages -----------------------------------------------------------------------------

    @GetMapping("/{chat_id}/messages")
    public @NonNull Page<ChatMessageDto> getMessages(
            @PathVariable("chat_id") @Positive long chatId,
            @PageableDefault(size = 50, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatMessageService.getMessages(extractUserId(authentication), chatId, pageable);
    }

    @PostMapping("/{chat_id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public @NonNull ChatMessageDto sendMessage(
            @PathVariable("chat_id") @Positive long chatId,
            @Valid @RequestBody SendMessageRequest request,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatMessageService.sendMessage(extractUserId(authentication), chatId, request);
    }

    // -------------- /chats/{id}/messages/{id} ------------------------------------------------------------------------

    @GetMapping("/{chat_id}/messages/{message_id}")
    public @NonNull ChatMessageDto getMessageById(
            @PathVariable("chat_id") @Positive long chatId,
            @PathVariable("message_id") @Positive long messageId,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatMessageService.getMessage(extractUserId(authentication), chatId, messageId);
    }

    @PatchMapping("/{chat_id}/messages/{message_id}")
    public @NonNull ChatMessageDto editMessageById(
            @PathVariable("chat_id") @Positive long chatId,
            @PathVariable("message_id") @Positive long messageId,
            @Valid @RequestBody EditMessageRequest request,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatMessageService.editMessage(extractUserId(authentication), chatId, messageId, request);
    }

    @PutMapping("/{chat_id}/messages/{message_id}")
    public @NonNull ChatMessageDto replaceMessageById(
            @PathVariable("chat_id") @Positive long chatId,
            @PathVariable("message_id") @Positive long messageId,
            @Valid @RequestBody EditMessageRequest request,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatMessageService.editMessage(extractUserId(authentication), chatId, messageId, request);
    }

    @DeleteMapping("/{chat_id}/messages/{message_id}")
    public @NonNull ChatMessageDto deleteMessage(
            @PathVariable("chat_id") @Positive long chatId,
            @PathVariable("message_id") @Positive long messageId,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatMessageService.deleteMessage(extractUserId(authentication), chatId, messageId);
    }

}
