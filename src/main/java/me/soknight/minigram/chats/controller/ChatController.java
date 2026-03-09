package me.soknight.minigram.chats.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.dto.ChatDto;
import me.soknight.minigram.chats.model.dto.ChatMemberDto;
import me.soknight.minigram.chats.model.dto.CreateChatRequest;
import me.soknight.minigram.chats.model.dto.MessageDto;
import me.soknight.minigram.chats.service.ChatService;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/chats")
@AllArgsConstructor
public class ChatController extends ApiControllerBase {

    private final ChatService chatService;

    @GetMapping
    public List<ChatDto> listChats(@Nullable Authentication authentication) throws ApiException {
        return chatService.listChats(extractUserId(authentication));
    }

    @GetMapping("/{id}")
    public ChatDto getChat(
            @PathVariable("id") @Positive long chatId,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatService.getChat(extractUserId(authentication), chatId);
    }

    @GetMapping("/{id}/messages")
    public List<MessageDto> getMessages(
            @PathVariable("id") @Positive long chatId,
            @RequestParam(name = "from", defaultValue = "0") @Min(0) int from,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatService.getMessages(extractUserId(authentication), chatId, from);
    }

    @PostMapping("/{id}/invite")
    public ChatMemberDto inviteUser(
            @PathVariable("id") @Positive long chatId,
            @RequestParam("user_id") @Positive long invitedUserId,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatService.inviteUser(extractUserId(authentication), chatId, invitedUserId);
    }

    @PostMapping
    public ResponseEntity<ChatDto> createChat(
            @Valid @RequestBody CreateChatRequest request,
            @Nullable Authentication authentication
    ) throws ApiException {
        ChatDto chat = chatService.createChat(extractUserId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(chat);
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveChat(
            @PathVariable("id") @Positive long chatId,
            @Nullable Authentication authentication
    ) throws ApiException {
        chatService.leaveChat(extractUserId(authentication), chatId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/kick")
    public ResponseEntity<Void> kickUser(
            @PathVariable("id") @Positive long chatId,
            @RequestParam("user_id") @Positive long kickedUserId,
            @Nullable Authentication authentication
    ) throws ApiException {
        chatService.kickUser(extractUserId(authentication), chatId, kickedUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChat(
            @PathVariable("id") @Positive long chatId,
            @Nullable Authentication authentication
    ) throws ApiException {
        chatService.deleteChat(extractUserId(authentication), chatId);
        return ResponseEntity.noContent().build();
    }

}
