package me.soknight.minigram.chats.service;

import jakarta.persistence.EntityManager;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.attribute.ChatMemberRole;
import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.model.dto.ChatDto;
import me.soknight.minigram.chats.model.request.CreateChatRequest;
import me.soknight.minigram.chats.service.client.TestProfileClient;
import me.soknight.minigram.chats.service.client.model.attribute.RelationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ChatServiceTest {

    private static final UUID USER_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER_3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID UNKNOWN_USER = UUID.fromString("00000000-0000-0000-0000-000000000999");

    @Autowired ChatService chatService;
    @Autowired TestProfileClient profileClient;
    @Autowired EntityManager em;

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

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    // --- createChat ---

    @Test
    void createSavedChat() throws ApiException {
        runAs(USER_1);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.SAVED, null, null));

        assertThat(chat.type()).isEqualTo(ChatType.SAVED);
        assertThat(chat.title()).isNull();
        assertThat(chat.ownerId()).isEqualTo(USER_1);
        assertThat(chat.members()).hasSize(1);
        assertThat(chat.members().getFirst().profileId()).isEqualTo(USER_1);
        assertThat(chat.members().getFirst().name()).isEqualTo("User 00000000");
        assertThat(chat.members().getFirst().photoUrl()).isEqualTo("https://example.com/" + USER_1 + ".png");
        assertThat(chat.members().getFirst().role()).isEqualTo(ChatMemberRole.OWNER);
    }

    @Test
    void createSavedChat_withMembers_throws() {
        runAs(USER_1);
        assertThatThrownBy(() -> chatService.createChat(new CreateChatRequest(ChatType.SAVED, null, List.of(USER_2))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void createDirectChat() throws ApiException {
        runAs(USER_1);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.DIRECT, null, List.of(USER_2)));

        assertThat(chat.type()).isEqualTo(ChatType.DIRECT);
        assertThat(chat.title()).isNull();
        assertThat(chat.members()).hasSize(2);
        assertThat(chat.members()).allSatisfy(member -> {
            assertThat(member.name()).isEqualTo("User 00000000");
            assertThat(member.photoUrl()).startsWith("https://example.com/");
        });
    }

    @Test
    void createDirectChat_wrongMemberCount_throws() {
        runAs(USER_1);
        assertThatThrownBy(() -> chatService.createChat(new CreateChatRequest(ChatType.DIRECT, null, List.of())))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> chatService.createChat(new CreateChatRequest(ChatType.DIRECT, null, List.of(USER_2, USER_3))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void createGroupChat() throws ApiException {
        runAs(USER_1);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.GROUP, "Test Group", List.of(USER_2, USER_3)));

        assertThat(chat.type()).isEqualTo(ChatType.GROUP);
        assertThat(chat.title()).isEqualTo("Test Group");
        assertThat(chat.members()).hasSize(3);
    }

    @Test
    void createGroupChat_withoutTitle_throws() {
        runAs(USER_1);
        assertThatThrownBy(() -> chatService.createChat(new CreateChatRequest(ChatType.GROUP, null, List.of(USER_2))))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> chatService.createChat(new CreateChatRequest(ChatType.GROUP, "  ", List.of(USER_2))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void createDirectChat_whenRelationNotAccepted_throws() {
        profileClient.setStatus(USER_2, RelationStatus.BLOCKED);
        runAs(USER_1);

        assertThatThrownBy(() -> chatService.createChat(new CreateChatRequest(ChatType.DIRECT, null, List.of(USER_2))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void createGroupChat_whenMemberRelationNotAccepted_throws() {
        profileClient.setStatus(USER_3, RelationStatus.NONE);
        runAs(USER_1);

        assertThatThrownBy(() -> chatService.createChat(new CreateChatRequest(ChatType.GROUP, "Test Group", List.of(USER_2, USER_3))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void createGroupChat_ownerExcludedFromMemberIds() throws ApiException {
        runAs(USER_1);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.GROUP, "Test", List.of(USER_1, USER_2)));

        assertThat(chat.members()).hasSize(2);
    }

    // --- getChats ---

    @Test
    void getChats_returnsOnlyUserChatsById() throws ApiException {
        runAs(USER_1);
        chatService.createChat(new CreateChatRequest(ChatType.SAVED, null, null));
        runAs(USER_2);
        chatService.createChat(new CreateChatRequest(ChatType.SAVED, null, null));
        runAs(USER_1);
        chatService.createChat(new CreateChatRequest(ChatType.DIRECT, null, List.of(USER_2)));
        flushAndClear();

        var defaultPage = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt", "id"));

        runAs(USER_1);
        var user1Chats = chatService.getChats(defaultPage);
        runAs(USER_2);
        var user2Chats = chatService.getChats(defaultPage);
        runAs(USER_3);
        var user3Chats = chatService.getChats(defaultPage);

        assertThat(user1Chats.getContent()).hasSize(2);
        assertThat(user1Chats.getTotalElements()).isEqualTo(2);
        assertThat(user2Chats.getContent()).hasSize(2);
        assertThat(user3Chats.getContent()).isEmpty();
    }

    // --- getChat ---

    @Test
    void getChat_ById_accessible() throws ApiException {
        runAs(USER_1);
        var created = chatService.createChat(new CreateChatRequest(ChatType.SAVED, null, null));
        flushAndClear();

        var chat = chatService.getChat(created.id());
        assertThat(chat.id()).isEqualTo(created.id());
    }

    @Test
    void getChat_ById_notAccessible_throws() throws ApiException {
        runAs(USER_1);
        var created = chatService.createChat(new CreateChatRequest(ChatType.SAVED, null, null));
        flushAndClear();

        runAs(UNKNOWN_USER);
        assertThatThrownBy(() -> chatService.getChat(created.id()))
                .isInstanceOf(ApiException.class);
    }

    // --- deleteChat ---

    @Test
    void deleteChat() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        chatService.deleteChat(chat.id());
        flushAndClear();

        assertThatThrownBy(() -> chatService.getChat(chat.id()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void deleteChat_notOwner_throws() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        runAs(USER_2);
        assertThatThrownBy(() -> chatService.deleteChat(chat.id()))
                .isInstanceOf(ApiException.class);
    }

    // --- helpers ---

    private ChatDto createGroup(UUID ownerId, UUID... memberIds) throws ApiException {
        runAs(ownerId);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.GROUP, "Test Group", List.of(memberIds)));
        flushAndClear();
        return chat;
    }

}
