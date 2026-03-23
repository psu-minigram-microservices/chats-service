package me.soknight.minigram.chats.controller;

import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.model.request.CreateChatRequest;
import me.soknight.minigram.chats.model.request.SendMessageRequest;
import me.soknight.minigram.chats.service.ChatMessageService;
import me.soknight.minigram.chats.service.ChatService;
import me.soknight.minigram.chats.service.client.TestProfileClient;
import me.soknight.minigram.chats.service.client.model.attribute.RelationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
class ChatMessageControllerApiTest {

    private static final UUID USER_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired MockMvc mockMvc;
    @Autowired ChatService chatService;
    @Autowired ChatMessageService messageService;
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
    void sendMessage_returnsCreatedMessage() throws Exception {
        long chatId = createDirectChat();

        mockMvc.perform(post("/api/v1/chats/{chatId}/messages", chatId)
                        .with(authUser(USER_1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "  hello  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("hello"))
                .andExpect(jsonPath("$.sender.profile_id").value(USER_1.toString()))
                .andExpect(jsonPath("$.sender.name").value("User 00000000"))
                .andExpect(jsonPath("$.sender.photoUrl").value("https://example.com/" + USER_1 + ".png"))
                .andExpect(jsonPath("$.chat.id").value(chatId));
    }

    @Test
    void sendMessage_withBlankContent_returnsValidationError() throws Exception {
        long chatId = createDirectChat();

        mockMvc.perform(post("/api/v1/chats/{chatId}/messages", chatId)
                        .with(authUser(USER_1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("incorrect_field_value"));
    }

    @Test
    void sendMessage_whenDirectRelationNotAccepted_returnsForbidden() throws Exception {
        long chatId = createDirectChat();
        profileClient.setStatus(USER_2, RelationStatus.BLOCKED);

        mockMvc.perform(post("/api/v1/chats/{chatId}/messages", chatId)
                        .with(authUser(USER_1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "hello"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code").value("relation_not_accepted"));
    }

    @Test
    void editMessage_returnsUpdatedMessage() throws Exception {
        long chatId = createDirectChat();
        runAs(USER_1);
        var message = messageService.sendMessage(USER_1, chatId, new SendMessageRequest("original"));

        mockMvc.perform(patch("/api/v1/chats/{chatId}/messages/{messageId}", chatId, message.id())
                        .with(authUser(USER_1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "updated"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(message.id()))
                .andExpect(jsonPath("$.content").value("updated"));
    }

    @Test
    void editMessage_byAnotherUser_returnsForbidden() throws Exception {
        long chatId = createDirectChat();
        runAs(USER_1);
        var message = messageService.sendMessage(USER_1, chatId, new SendMessageRequest("original"));

        mockMvc.perform(patch("/api/v1/chats/{chatId}/messages/{messageId}", chatId, message.id())
                        .with(authUser(USER_2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "updated"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code").value("access_denied"));
    }

    @Test
    void getMessages_paginationWithCustomPageSize() throws Exception {
        long chatId = createDirectChat();
        for (int i = 0; i < 5; i++)
            messageService.sendMessage(USER_1, chatId, new SendMessageRequest("message " + i));

        mockMvc.perform(get("/api/v1/chats/{chatId}/messages", chatId)
                        .with(authUser(USER_1))
                        .queryParam("page", "0")
                        .queryParam("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(3));

        mockMvc.perform(get("/api/v1/chats/{chatId}/messages", chatId)
                        .with(authUser(USER_1))
                        .queryParam("page", "1")
                        .queryParam("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    void getMessages_defaultSortIsNewestFirst() throws Exception {
        long chatId = createDirectChat();
        var first = messageService.sendMessage(USER_1, chatId, new SendMessageRequest("first"));
        var second = messageService.sendMessage(USER_1, chatId, new SendMessageRequest("second"));

        mockMvc.perform(get("/api/v1/chats/{chatId}/messages", chatId)
                        .with(authUser(USER_1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(second.id()))
                .andExpect(jsonPath("$.content[1].id").value(first.id()));
    }

    @Test
    void deleteMessage_returnsDeletedMessage() throws Exception {
        long chatId = createDirectChat();
        runAs(USER_1);
        var message = messageService.sendMessage(USER_1, chatId, new SendMessageRequest("to delete"));

        mockMvc.perform(delete("/api/v1/chats/{chatId}/messages/{messageId}", chatId, message.id())
                        .with(authUser(USER_1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(message.id()));

        mockMvc.perform(get("/api/v1/chats/{chatId}/messages", chatId)
                        .with(authUser(USER_1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    private long createDirectChat() throws ApiException {
        runAs(USER_1);
        return chatService.createChat(USER_1, new CreateChatRequest(ChatType.DIRECT, null, List.of(USER_2))).id();
    }

    private RequestPostProcessor authUser(UUID userId) {
        return user(userId.toString());
    }

}
