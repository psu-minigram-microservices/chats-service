package me.soknight.minigram.chats.controller;

import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.model.request.CreateChatRequest;
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

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatControllerApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired ChatService chatService;

    @Test
    void createChat_returnsCreatedChat() throws Exception {
        mockMvc.perform(post("/api/v1/chats")
                        .with(authUser(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "group",
                                  "title": "  Team Chat  ",
                                  "member_ids": [2, 3]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("group"))
                .andExpect(jsonPath("$.title").value("Team Chat"))
                .andExpect(jsonPath("$.owner_id").value(1))
                .andExpect(jsonPath("$.members[*].user_id", hasItem(1)))
                .andExpect(jsonPath("$.members[*].user_id", hasItem(2)))
                .andExpect(jsonPath("$.members[*].user_id", hasItem(3)));
    }

    @Test
    void createChat_withoutType_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/chats")
                        .with(authUser(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Any"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("incorrect_field_value"));
    }

    @Test
    void listChats_returnsOnlyUserChats() throws Exception {
        chatService.createChat(1L, new CreateChatRequest(ChatType.SAVED, null, null));
        chatService.createChat(2L, new CreateChatRequest(ChatType.SAVED, null, null));
        chatService.createChat(1L, new CreateChatRequest(ChatType.DIRECT, null, List.of(2L)));

        mockMvc.perform(get("/api/v1/chats").with(authUser(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/v1/chats").with(authUser(3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listChats_paginationWithCustomPageSize() throws Exception {
        for (int i = 0; i < 5; i++)
            chatService.createChat(1L, new CreateChatRequest(ChatType.GROUP, "Chat " + i, List.of(2L)));

        mockMvc.perform(get("/api/v1/chats")
                        .with(authUser(1))
                        .queryParam("page", "0")
                        .queryParam("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(3));

        mockMvc.perform(get("/api/v1/chats")
                        .with(authUser(1))
                        .queryParam("page", "1")
                        .queryParam("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    void listChats_emptyPage_beyondLastPage() throws Exception {
        chatService.createChat(1L, new CreateChatRequest(ChatType.SAVED, null, null));

        mockMvc.perform(get("/api/v1/chats")
                        .with(authUser(1))
                        .queryParam("page", "5")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listChats_withNonNumericPrincipal_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/chats").with(user("abc")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("invalid_token_subject"));
    }

    private RequestPostProcessor authUser(long userId) {
        return user(Long.toString(userId));
    }

}
