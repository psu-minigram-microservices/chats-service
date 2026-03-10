package me.soknight.minigram.chats.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.dto.ChatMemberDto;
import me.soknight.minigram.chats.service.ChatMemberService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/chats")
@AllArgsConstructor
@Tag(name = "Chat Members", description = "Manage members in chats: invite, view, kick and leave")
public class ChatMemberController extends ApiControllerBase {

    private final @NonNull ChatMemberService chatMemberService;

    // -------------- /chats/{id}/members ------------------------------------------------------------------------------

    @GetMapping("/{chat_id}/members")
    public @NonNull Page<ChatMemberDto> getMembers(
            @PathVariable("chat_id") @Positive long chatId,
            @PageableDefault(size = 50, sort = {"joinedAt", "id.userId"}, direction = Sort.Direction.DESC) Pageable pageable,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatMemberService.getMembers(extractUserId(authentication), chatId, pageable);
    }

    @PostMapping("/{chat_id}/members/{user_id}")
    public @NonNull ChatMemberDto inviteUser(
            @PathVariable("chat_id") @Positive long chatId,
            @PathVariable("user_id") UUID userId,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatMemberService.inviteUser(extractUserId(authentication), chatId, userId);
    }

    // -------------- /chats/{id}/members/{id} -------------------------------------------------------------------------

    @GetMapping("/{chat_id}/members/me")
    public @NonNull ChatMemberDto getMemberMe(
            @PathVariable("chat_id") @Positive long chatId,
            @Nullable Authentication authentication
    ) throws ApiException {
        UUID actorUserId = extractUserId(authentication);
        return chatMemberService.getMember(actorUserId, chatId, actorUserId);
    }

    @GetMapping("/{chat_id}/members/{member_id}")
    public @NonNull ChatMemberDto getMemberById(
            @PathVariable("chat_id") @Positive long chatId,
            @PathVariable("member_id") UUID memberId,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatMemberService.getMember(extractUserId(authentication), chatId, memberId);
    }

    @DeleteMapping("/{chat_id}/members/me")
    public @NonNull ChatMemberDto leaveChat(
            @PathVariable("chat_id") @Positive long chatId,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatMemberService.leaveChat(extractUserId(authentication), chatId);
    }

    @DeleteMapping("/{chat_id}/members/{member_id}")
    public @NonNull ChatMemberDto kickUser(
            @PathVariable("chat_id") @Positive long chatId,
            @PathVariable("member_id") UUID memberId,
            @Nullable Authentication authentication
    ) throws ApiException {
        return chatMemberService.kickUser(extractUserId(authentication), chatId, memberId);
    }

}
