package me.soknight.minigram.chats.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.dto.ChatMemberDto;
import me.soknight.minigram.chats.service.ChatMemberService;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/chats")
@AllArgsConstructor
@Tag(name = "Chat Members", description = "Manage members in chats: invite, view, kick and leave")
public class ChatMemberController {

    private final @NonNull ChatMemberService chatMemberService;

    // -------------- /chats/{id}/members ------------------------------------------------------------------------------

    @GetMapping("/{chat_id}/members")
    public @NonNull Page<ChatMemberDto> getMembers(
            @PathVariable("chat_id") @Positive long chatId,
            @PageableDefault(size = 50, sort = {"joinedAt", "id.profileId"}, direction = Sort.Direction.DESC) Pageable pageable
    ) throws ApiException {
        return chatMemberService.getMembers(chatId, pageable);
    }

    @PostMapping("/{chat_id}/members/{profile_id}")
    public @NonNull ChatMemberDto inviteUser(
            @PathVariable("chat_id") @Positive long chatId,
            @PathVariable("profile_id") UUID profileId
    ) throws ApiException {
        return chatMemberService.inviteUser(chatId, profileId);
    }

    // -------------- /chats/{id}/members/{id} -------------------------------------------------------------------------

    @GetMapping("/{chat_id}/members/me")
    public @NonNull ChatMemberDto getMemberMe(
            @PathVariable("chat_id") @Positive long chatId
    ) throws ApiException {
        return chatMemberService.getMember(chatId, null);
    }

    @GetMapping("/{chat_id}/members/{member_id}")
    public @NonNull ChatMemberDto getMemberById(
            @PathVariable("chat_id") @Positive long chatId,
            @PathVariable("member_id") UUID memberId
    ) throws ApiException {
        return chatMemberService.getMember(chatId, memberId);
    }

    @DeleteMapping("/{chat_id}/members/me")
    public @NonNull ChatMemberDto leaveChat(
            @PathVariable("chat_id") @Positive long chatId
    ) throws ApiException {
        return chatMemberService.leaveChat(chatId);
    }

    @DeleteMapping("/{chat_id}/members/{member_id}")
    public @NonNull ChatMemberDto kickUser(
            @PathVariable("chat_id") @Positive long chatId,
            @PathVariable("member_id") UUID memberId
    ) throws ApiException {
        return chatMemberService.kickUser(chatId, memberId);
    }

}
