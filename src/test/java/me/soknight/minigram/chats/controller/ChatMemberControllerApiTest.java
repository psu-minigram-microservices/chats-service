package me.soknight.minigram.chats.controller;

import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.model.request.CreateChatRequest;
import me.soknight.minigram.chats.service.ChatService;
import me.soknight.minigram.chats.service.client.TestProfileClient;
import me.soknight.minigram.chats.service.client.model.attribute.RelationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatMemberControllerApiTest {

    private static final UUID USER_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER_3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Autowired MockMvc mockMvc;
    @Autowired ChatService chatService;
    @Autowired TestProfileClient profileClient;

    @BeforeEach
    void resetRelations() {
        profileClient.reset();
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private void runAs(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(userId.toString(), null)
        );
    }

    @Test
    void inviteUser_toGroupChat_returnsMember() throws Exception {
        runAs(USER_1);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.GROUP, "Test", List.of(USER_2)));

        mockMvc.perform(post("/api/v1/chats/{chatId}/members/{userId}", chat.id(), USER_3)
                        .with(authUser(USER_1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile_id").value(USER_3.toString()))
                .andExpect(jsonPath("$.name").value("User 00000000"))
                .andExpect(jsonPath("$.photoUrl").value("https://example.com/" + USER_3 + ".png"))
                .andExpect(jsonPath("$.role").value("member"));
    }

    @Test
    void inviteUser_toDirectChat_returnsConflict() throws Exception {
        runAs(USER_1);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.DIRECT, null, List.of(USER_2)));

        mockMvc.perform(post("/api/v1/chats/{chatId}/members/{userId}", chat.id(), USER_3)
                        .with(authUser(USER_1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("chat_invite_not_supported"));
    }

    @Test
    void inviteUser_whenRelationNotAccepted_returnsForbidden() throws Exception {
        runAs(USER_1);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.GROUP, "Test", List.of(USER_2)));
        profileClient.setStatus(USER_3, RelationStatus.NONE);

        mockMvc.perform(post("/api/v1/chats/{chatId}/members/{userId}", chat.id(), USER_3)
                        .with(authUser(USER_1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code").value("relation_not_accepted"));
    }

    @Test
    void getMembers_returnsMembersPage() throws Exception {
        runAs(USER_1);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.GROUP, "Test", List.of(USER_2, USER_3)));

        mockMvc.perform(get("/api/v1/chats/{chatId}/members", chat.id())
                        .with(authUser(USER_1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void getMemberMe_returnsCurrentUser() throws Exception {
        runAs(USER_1);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.GROUP, "Test", List.of(USER_2)));

        mockMvc.perform(get("/api/v1/chats/{chatId}/members/me", chat.id())
                        .with(authUser(USER_1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile_id").value(USER_1.toString()))
                .andExpect(jsonPath("$.name").value("User 00000000"))
                .andExpect(jsonPath("$.photoUrl").value("https://example.com/" + USER_1 + ".png"))
                .andExpect(jsonPath("$.role").value("owner"));
    }

    @Test
    void getMemberById_returnsMember() throws Exception {
        runAs(USER_1);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.GROUP, "Test", List.of(USER_2)));

        mockMvc.perform(get("/api/v1/chats/{chatId}/members/{memberId}", chat.id(), USER_2)
                        .with(authUser(USER_1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile_id").value(USER_2.toString()))
                .andExpect(jsonPath("$.name").value("User 00000000"))
                .andExpect(jsonPath("$.photoUrl").value("https://example.com/" + USER_2 + ".png"))
                .andExpect(jsonPath("$.role").value("member"));
    }

    @Test
    void leaveChat_returnsMember() throws Exception {
        runAs(USER_1);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.GROUP, "Test", List.of(USER_2)));

        mockMvc.perform(delete("/api/v1/chats/{chatId}/members/me", chat.id())
                        .with(authUser(USER_2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile_id").value(USER_2.toString()));
    }

    @Test
    void leaveChat_owner_returnsConflict() throws Exception {
        runAs(USER_1);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.GROUP, "Test", List.of(USER_2)));

        mockMvc.perform(delete("/api/v1/chats/{chatId}/members/me", chat.id())
                        .with(authUser(USER_1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("owner_cannot_leave_chat"));
    }

    @Test
    void kickUser_returnsMember() throws Exception {
        runAs(USER_1);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.GROUP, "Test", List.of(USER_2, USER_3)));

        mockMvc.perform(delete("/api/v1/chats/{chatId}/members/{memberId}", chat.id(), USER_2)
                        .with(authUser(USER_1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile_id").value(USER_2.toString()));
    }

    @Test
    void kickUser_notOwner_returnsForbidden() throws Exception {
        runAs(USER_1);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.GROUP, "Test", List.of(USER_2, USER_3)));

        mockMvc.perform(delete("/api/v1/chats/{chatId}/members/{memberId}", chat.id(), USER_3)
                        .with(authUser(USER_2)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code").value("access_denied"));
    }

    private RequestPostProcessor authUser(UUID userId) {
        return user(userId.toString());
    }

}
