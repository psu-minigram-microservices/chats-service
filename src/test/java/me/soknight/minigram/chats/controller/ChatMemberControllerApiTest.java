package me.soknight.minigram.chats.controller;

import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.model.request.CreateChatRequest;
import me.soknight.minigram.chats.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatMemberControllerApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired ChatService chatService;

    @Test
    void inviteUser_toGroupChat_returnsMember() throws Exception {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.GROUP, "Test", List.of(2L)));

        mockMvc.perform(post("/api/v1/chats/{chatId}/members/{userId}", chat.id(), 3L)
                        .with(authUser(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value(3))
                .andExpect(jsonPath("$.role").value("member"));
    }

    @Test
    void inviteUser_toDirectChat_returnsConflict() throws Exception {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.DIRECT, null, List.of(2L)));

        mockMvc.perform(post("/api/v1/chats/{chatId}/members/{userId}", chat.id(), 3L)
                        .with(authUser(1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("chat_invite_not_supported"));
    }

    @Test
    void getMembers_returnsMembersPage() throws Exception {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.GROUP, "Test", List.of(2L, 3L)));

        mockMvc.perform(get("/api/v1/chats/{chatId}/members", chat.id())
                        .with(authUser(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void getMemberMe_returnsCurrentUser() throws Exception {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.GROUP, "Test", List.of(2L)));

        mockMvc.perform(get("/api/v1/chats/{chatId}/members/me", chat.id())
                        .with(authUser(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value(1))
                .andExpect(jsonPath("$.role").value("owner"));
    }

    @Test
    void getMemberById_returnsMember() throws Exception {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.GROUP, "Test", List.of(2L)));

        mockMvc.perform(get("/api/v1/chats/{chatId}/members/{memberId}", chat.id(), 2L)
                        .with(authUser(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value(2))
                .andExpect(jsonPath("$.role").value("member"));
    }

    @Test
    void leaveChat_returnsMember() throws Exception {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.GROUP, "Test", List.of(2L)));

        mockMvc.perform(delete("/api/v1/chats/{chatId}/members/me", chat.id())
                        .with(authUser(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value(2));
    }

    @Test
    void leaveChat_owner_returnsConflict() throws Exception {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.GROUP, "Test", List.of(2L)));

        mockMvc.perform(delete("/api/v1/chats/{chatId}/members/me", chat.id())
                        .with(authUser(1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("owner_cannot_leave_chat"));
    }

    @Test
    void kickUser_returnsMember() throws Exception {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.GROUP, "Test", List.of(2L, 3L)));

        mockMvc.perform(delete("/api/v1/chats/{chatId}/members/{memberId}", chat.id(), 2L)
                        .with(authUser(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value(2));
    }

    @Test
    void kickUser_notOwner_returnsForbidden() throws Exception {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.GROUP, "Test", List.of(2L, 3L)));

        mockMvc.perform(delete("/api/v1/chats/{chatId}/members/{memberId}", chat.id(), 3L)
                        .with(authUser(2)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code").value("access_denied"));
    }

    private RequestPostProcessor authUser(long userId) {
        return user(Long.toString(userId));
    }

}
