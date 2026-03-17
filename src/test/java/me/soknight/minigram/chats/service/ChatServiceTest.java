package me.soknight.minigram.chats.service;

import jakarta.persistence.EntityManager;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.attribute.ChatMemberRole;
import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.model.attribute.RelationStatus;
import me.soknight.minigram.chats.model.dto.ChatDto;
import me.soknight.minigram.chats.model.request.CreateChatRequest;
import me.soknight.minigram.chats.service.client.TestProfileRelationsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    @Autowired TestProfileRelationsClient profileRelationsClient;
    @Autowired EntityManager em;

    @BeforeEach
    void resetRelations() {
        profileRelationsClient.reset();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    // --- createChat ---

    @Test
    void createSavedChat() throws ApiException {
        var chat = chatService.createChat(USER_1, new CreateChatRequest(ChatType.SAVED, null, null));

        assertThat(chat.type()).isEqualTo(ChatType.SAVED);
        assertThat(chat.title()).isNull();
        assertThat(chat.ownerId()).isEqualTo(USER_1);
        assertThat(chat.members()).hasSize(1);
        assertThat(chat.members().getFirst().userId()).isEqualTo(USER_1);
        assertThat(chat.members().getFirst().role()).isEqualTo(ChatMemberRole.OWNER);
    }

    @Test
    void createSavedChat_withMembers_throws() {
        assertThatThrownBy(() -> chatService.createChat(USER_1, new CreateChatRequest(ChatType.SAVED, null, List.of(USER_2))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void createDirectChat() throws ApiException {
        var chat = chatService.createChat(USER_1, new CreateChatRequest(ChatType.DIRECT, null, List.of(USER_2)));

        assertThat(chat.type()).isEqualTo(ChatType.DIRECT);
        assertThat(chat.title()).isNull();
        assertThat(chat.members()).hasSize(2);
    }

    @Test
    void createDirectChat_wrongMemberCount_throws() {
        assertThatThrownBy(() -> chatService.createChat(USER_1, new CreateChatRequest(ChatType.DIRECT, null, List.of())))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> chatService.createChat(USER_1, new CreateChatRequest(ChatType.DIRECT, null, List.of(USER_2, USER_3))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void createGroupChat() throws ApiException {
        var chat = chatService.createChat(USER_1, new CreateChatRequest(ChatType.GROUP, "Test Group", List.of(USER_2, USER_3)));

        assertThat(chat.type()).isEqualTo(ChatType.GROUP);
        assertThat(chat.title()).isEqualTo("Test Group");
        assertThat(chat.members()).hasSize(3);
    }

    @Test
    void createGroupChat_withoutTitle_throws() {
        assertThatThrownBy(() -> chatService.createChat(USER_1, new CreateChatRequest(ChatType.GROUP, null, List.of(USER_2))))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> chatService.createChat(USER_1, new CreateChatRequest(ChatType.GROUP, "  ", List.of(USER_2))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void createDirectChat_whenRelationNotAccepted_throws() {
        profileRelationsClient.setStatus(USER_2, RelationStatus.BLOCKED);

        assertThatThrownBy(() -> chatService.createChat(USER_1, new CreateChatRequest(ChatType.DIRECT, null, List.of(USER_2))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void createGroupChat_whenMemberRelationNotAccepted_throws() {
        profileRelationsClient.setStatus(USER_3, RelationStatus.PENDING);

        assertThatThrownBy(() -> chatService.createChat(USER_1, new CreateChatRequest(ChatType.GROUP, "Test Group", List.of(USER_2, USER_3))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void createGroupChat_ownerExcludedFromMemberIds() throws ApiException {
        var chat = chatService.createChat(USER_1, new CreateChatRequest(ChatType.GROUP, "Test", List.of(USER_1, USER_2)));

        assertThat(chat.members()).hasSize(2);
    }

    // --- getChats ---

    @Test
    void getChats_returnsOnlyUserChatsById() throws ApiException {
        chatService.createChat(USER_1, new CreateChatRequest(ChatType.SAVED, null, null));
        chatService.createChat(USER_2, new CreateChatRequest(ChatType.SAVED, null, null));
        chatService.createChat(USER_1, new CreateChatRequest(ChatType.DIRECT, null, List.of(USER_2)));
        flushAndClear();

        var defaultPage = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt", "id"));

        var user1Chats = chatService.getChats(USER_1, defaultPage);
        var user2Chats = chatService.getChats(USER_2, defaultPage);
        var user3Chats = chatService.getChats(USER_3, defaultPage);

        assertThat(user1Chats.getContent()).hasSize(2);
        assertThat(user1Chats.getTotalElements()).isEqualTo(2);
        assertThat(user2Chats.getContent()).hasSize(2);
        assertThat(user3Chats.getContent()).isEmpty();
    }

    // --- getChat ---

    @Test
    void getChat_ById_accessible() throws ApiException {
        var created = chatService.createChat(USER_1, new CreateChatRequest(ChatType.SAVED, null, null));
        flushAndClear();

        var chat = chatService.getChat(USER_1, created.id());
        assertThat(chat.id()).isEqualTo(created.id());
    }

    @Test
    void getChat_ById_notAccessible_throws() throws ApiException {
        var created = chatService.createChat(USER_1, new CreateChatRequest(ChatType.SAVED, null, null));
        flushAndClear();

        assertThatThrownBy(() -> chatService.getChat(UNKNOWN_USER, created.id()))
                .isInstanceOf(ApiException.class);
    }

    // --- deleteChat ---

    @Test
    void deleteChat() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        chatService.deleteChat(USER_1, chat.id());
        flushAndClear();

        assertThatThrownBy(() -> chatService.getChat(USER_1, chat.id()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void deleteChat_notOwner_throws() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        assertThatThrownBy(() -> chatService.deleteChat(USER_2, chat.id()))
                .isInstanceOf(ApiException.class);
    }

    // --- helpers ---

    private ChatDto createGroup(UUID ownerId, UUID... memberIds) throws ApiException {
        var chat = chatService.createChat(ownerId, new CreateChatRequest(ChatType.GROUP, "Test Group", List.of(memberIds)));
        flushAndClear();
        return chat;
    }

}
