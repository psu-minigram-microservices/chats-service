package me.soknight.minigram.chats.controller;

import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.model.attribute.RelationStatus;
import me.soknight.minigram.chats.model.request.CreateChatRequest;
import me.soknight.minigram.chats.service.ChatService;
import me.soknight.minigram.chats.service.client.TestProfileRelationsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    private static final UUID USER_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER_3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Autowired MockMvc mockMvc;
    @Autowired ChatService chatService;
    @Autowired TestProfileRelationsClient profileRelationsClient;

    @BeforeEach
    void resetRelations() {
        profileRelationsClient.reset();
    }

    @Test
    void createChat_returnsCreatedChat() throws Exception {
        mockMvc.perform(post("/api/v1/chats")
                        .with(authUser(USER_1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "group",
                                  "title": "  Team Chat  ",
                                  "member_ids": ["%s", "%s"]
                                }
                                """.formatted(USER_2, USER_3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("group"))
                .andExpect(jsonPath("$.title").value("Team Chat"))
                .andExpect(jsonPath("$.owner_id").value(USER_1.toString()))
                .andExpect(jsonPath("$.members[*].user_id", hasItem(USER_1.toString())))
                .andExpect(jsonPath("$.members[*].user_id", hasItem(USER_2.toString())))
                .andExpect(jsonPath("$.members[*].user_id", hasItem(USER_3.toString())));
    }

    @Test
    void createChat_withoutType_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/chats")
                        .with(authUser(USER_1))
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
    void createDirectChat_whenRelationNotAccepted_returnsForbidden() throws Exception {
        profileRelationsClient.setStatus(USER_2, RelationStatus.NONE);

        mockMvc.perform(post("/api/v1/chats")
                        .with(authUser(USER_1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "direct",
                                  "member_ids": ["%s"]
                                }
                                """.formatted(USER_2)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code").value("relation_not_accepted"));
    }

    @Test
    void createGroupChat_whenAnyMemberRelationNotAccepted_returnsForbidden() throws Exception {
        profileRelationsClient.setStatus(USER_3, RelationStatus.BLOCKED);

        mockMvc.perform(post("/api/v1/chats")
                        .with(authUser(USER_1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "group",
                                  "title": "Team Chat",
                                  "member_ids": ["%s", "%s"]
                                }
                                """.formatted(USER_2, USER_3)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code").value("relation_not_accepted"));
    }

    @Test
    void listChats_returnsOnlyUserChats() throws Exception {
        chatService.createChat(USER_1, new CreateChatRequest(ChatType.SAVED, null, null));
        chatService.createChat(USER_2, new CreateChatRequest(ChatType.SAVED, null, null));
        chatService.createChat(USER_1, new CreateChatRequest(ChatType.DIRECT, null, List.of(USER_2)));

        mockMvc.perform(get("/api/v1/chats").with(authUser(USER_1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/v1/chats").with(authUser(USER_3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listChats_paginationWithCustomPageSize() throws Exception {
        for (int i = 0; i < 5; i++)
            chatService.createChat(USER_1, new CreateChatRequest(ChatType.GROUP, "Chat " + i, List.of(USER_2)));

        mockMvc.perform(get("/api/v1/chats")
                        .with(authUser(USER_1))
                        .queryParam("page", "0")
                        .queryParam("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(3));

        mockMvc.perform(get("/api/v1/chats")
                        .with(authUser(USER_1))
                        .queryParam("page", "1")
                        .queryParam("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    void listChats_emptyPage_beyondLastPage() throws Exception {
        chatService.createChat(USER_1, new CreateChatRequest(ChatType.SAVED, null, null));

        mockMvc.perform(get("/api/v1/chats")
                        .with(authUser(USER_1))
                        .queryParam("page", "5")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listChats_withNonUuidPrincipal_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/chats").with(user("abc")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("invalid_token_subject"));
    }

    private RequestPostProcessor authUser(UUID userId) {
        return user(userId.toString());
    }

}
