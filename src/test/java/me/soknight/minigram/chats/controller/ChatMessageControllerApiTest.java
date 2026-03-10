package me.soknight.minigram.chats.controller;

import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.model.request.CreateChatRequest;
import me.soknight.minigram.chats.model.request.SendMessageRequest;
import me.soknight.minigram.chats.service.ChatMessageService;
import me.soknight.minigram.chats.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatMessageControllerApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired ChatService chatService;
    @Autowired ChatMessageService messageService;

    @Test
    void sendMessage_returnsCreatedMessage() throws Exception {
        long chatId = createDirectChat();

        mockMvc.perform(post("/api/v1/chats/{chatId}/messages", chatId)
                        .with(authUser(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "  hello  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("hello"))
                .andExpect(jsonPath("$.sender.user_id").value(1))
                .andExpect(jsonPath("$.chat.id").value(chatId));
    }

    @Test
    void sendMessage_withBlankContent_returnsValidationError() throws Exception {
        long chatId = createDirectChat();

        mockMvc.perform(post("/api/v1/chats/{chatId}/messages", chatId)
                        .with(authUser(1))
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
    void editMessage_returnsUpdatedMessage() throws Exception {
        long chatId = createDirectChat();
        var message = messageService.sendMessage(1L, chatId, new SendMessageRequest("original"));

        mockMvc.perform(patch("/api/v1/chats/{chatId}/messages/{messageId}", chatId, message.id())
                        .with(authUser(1))
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
        var message = messageService.sendMessage(1L, chatId, new SendMessageRequest("original"));

        mockMvc.perform(patch("/api/v1/chats/{chatId}/messages/{messageId}", chatId, message.id())
                        .with(authUser(2))
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
            messageService.sendMessage(1L, chatId, new SendMessageRequest("message " + i));

        mockMvc.perform(get("/api/v1/chats/{chatId}/messages", chatId)
                        .with(authUser(1))
                        .queryParam("page", "0")
                        .queryParam("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(3));

        mockMvc.perform(get("/api/v1/chats/{chatId}/messages", chatId)
                        .with(authUser(1))
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
        var first = messageService.sendMessage(1L, chatId, new SendMessageRequest("first"));
        var second = messageService.sendMessage(1L, chatId, new SendMessageRequest("second"));

        mockMvc.perform(get("/api/v1/chats/{chatId}/messages", chatId)
                        .with(authUser(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(second.id()))
                .andExpect(jsonPath("$.content[1].id").value(first.id()));
    }

    @Test
    void deleteMessage_returnsDeletedMessage() throws Exception {
        long chatId = createDirectChat();
        var message = messageService.sendMessage(1L, chatId, new SendMessageRequest("to delete"));

        mockMvc.perform(delete("/api/v1/chats/{chatId}/messages/{messageId}", chatId, message.id())
                        .with(authUser(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(message.id()));

        mockMvc.perform(get("/api/v1/chats/{chatId}/messages", chatId)
                        .with(authUser(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    private long createDirectChat() throws ApiException {
        return chatService.createChat(1L, new CreateChatRequest(ChatType.DIRECT, null, List.of(2L))).id();
    }

    private RequestPostProcessor authUser(long userId) {
        return user(Long.toString(userId));
    }

}
