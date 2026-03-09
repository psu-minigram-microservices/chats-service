package me.soknight.minigram.chats.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.request.EditMessageRequest;
import me.soknight.minigram.chats.model.dto.MessageDto;
import me.soknight.minigram.chats.model.request.SendMessageRequest;
import me.soknight.minigram.chats.service.MessageService;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class MessageController extends ApiControllerBase {

    private final MessageService messageService;

    @PostMapping("/chats/{id}/send")
    public ResponseEntity<MessageDto> sendMessage(
            @PathVariable("id") @Positive long chatId,
            @Valid @RequestBody SendMessageRequest request,
            @Nullable Authentication authentication
    ) throws ApiException {
        MessageDto message = messageService.sendMessage(extractUserId(authentication), chatId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @RequestMapping(path = "/messages/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public MessageDto editMessage(
            @PathVariable("id") @Positive long messageId,
            @Valid @RequestBody EditMessageRequest request,
            @Nullable Authentication authentication
    ) throws ApiException {
        return messageService.editMessage(extractUserId(authentication), messageId, request);
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable("id") @Positive long messageId,
            @Nullable Authentication authentication
    ) throws ApiException {
        messageService.deleteMessage(extractUserId(authentication), messageId);
        return ResponseEntity.noContent().build();
    }

}
