package me.soknight.minigram.chats.service;

import jakarta.persistence.EntityManager;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.attribute.ChatMemberRole;
import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.model.dto.ChatDto;
import me.soknight.minigram.chats.model.request.CreateChatRequest;
import me.soknight.minigram.chats.model.request.SendMessageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ChatServiceTest {

    @Autowired ChatService chatService;
    @Autowired MessageService messageService;
    @Autowired EntityManager em;

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    // --- createChat ---

    @Test
    void createSavedChat() throws ApiException {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.SAVED, null, null));

        assertThat(chat.type()).isEqualTo(ChatType.SAVED);
        assertThat(chat.title()).isNull();
        assertThat(chat.ownerId()).isEqualTo(1L);
        assertThat(chat.members()).hasSize(1);
        assertThat(chat.members().getFirst().userId()).isEqualTo(1L);
        assertThat(chat.members().getFirst().role()).isEqualTo(ChatMemberRole.OWNER);
    }

    @Test
    void createSavedChat_withMembers_throws() {
        assertThatThrownBy(() -> chatService.createChat(1L, new CreateChatRequest(ChatType.SAVED, null, List.of(2L))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void createDirectChat() throws ApiException {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.DIRECT, null, List.of(2L)));

        assertThat(chat.type()).isEqualTo(ChatType.DIRECT);
        assertThat(chat.title()).isNull();
        assertThat(chat.members()).hasSize(2);
    }

    @Test
    void createDirectChat_wrongMemberCount_throws() {
        assertThatThrownBy(() -> chatService.createChat(1L, new CreateChatRequest(ChatType.DIRECT, null, List.of())))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> chatService.createChat(1L, new CreateChatRequest(ChatType.DIRECT, null, List.of(2L, 3L))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void createGroupChat() throws ApiException {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.GROUP, "Test Group", List.of(2L, 3L)));

        assertThat(chat.type()).isEqualTo(ChatType.GROUP);
        assertThat(chat.title()).isEqualTo("Test Group");
        assertThat(chat.members()).hasSize(3);
    }

    @Test
    void createGroupChat_withoutTitle_throws() {
        assertThatThrownBy(() -> chatService.createChat(1L, new CreateChatRequest(ChatType.GROUP, null, List.of(2L))))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> chatService.createChat(1L, new CreateChatRequest(ChatType.GROUP, "  ", List.of(2L))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void createGroupChat_ownerExcludedFromMemberIds() throws ApiException {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.GROUP, "Test", List.of(1L, 2L)));

        assertThat(chat.members()).hasSize(2);
    }

    // --- listChats ---

    @Test
    void listChats_returnsOnlyUserChats() throws ApiException {
        chatService.createChat(1L, new CreateChatRequest(ChatType.SAVED, null, null));
        chatService.createChat(2L, new CreateChatRequest(ChatType.SAVED, null, null));
        chatService.createChat(1L, new CreateChatRequest(ChatType.DIRECT, null, List.of(2L)));
        flushAndClear();

        var defaultPage = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt", "id"));

        var user1Chats = chatService.listChats(1L, defaultPage);
        var user2Chats = chatService.listChats(2L, defaultPage);
        var user3Chats = chatService.listChats(3L, defaultPage);

        assertThat(user1Chats.getContent()).hasSize(2);
        assertThat(user1Chats.getTotalElements()).isEqualTo(2);
        assertThat(user2Chats.getContent()).hasSize(2);
        assertThat(user3Chats.getContent()).isEmpty();
    }

    // --- getChat ---

    @Test
    void getChat_accessible() throws ApiException {
        var created = chatService.createChat(1L, new CreateChatRequest(ChatType.SAVED, null, null));
        flushAndClear();

        var chat = chatService.getChat(1L, created.id());
        assertThat(chat.id()).isEqualTo(created.id());
    }

    @Test
    void getChat_notAccessible_throws() throws ApiException {
        var created = chatService.createChat(1L, new CreateChatRequest(ChatType.SAVED, null, null));
        flushAndClear();

        assertThatThrownBy(() -> chatService.getChat(999L, created.id()))
                .isInstanceOf(ApiException.class);
    }

    // --- inviteUser ---

    @Test
    void inviteUser() throws ApiException {
        var chat = createGroup(1L, 2L);

        var member = chatService.inviteUser(1L, chat.id(), 3L);

        assertThat(member.userId()).isEqualTo(3L);
        assertThat(member.role()).isEqualTo(ChatMemberRole.MEMBER);
        flushAndClear();

        var updated = chatService.getChat(1L, chat.id());
        assertThat(updated.members()).hasSize(3);
    }

    @Test
    void inviteUser_notGroup_throws() throws ApiException {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.DIRECT, null, List.of(2L)));
        flushAndClear();

        assertThatThrownBy(() -> chatService.inviteUser(1L, chat.id(), 3L))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void inviteUser_notOwner_throws() throws ApiException {
        var chat = createGroup(1L, 2L);

        assertThatThrownBy(() -> chatService.inviteUser(2L, chat.id(), 3L))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void inviteUser_alreadyMember_throws() throws ApiException {
        var chat = createGroup(1L, 2L);

        assertThatThrownBy(() -> chatService.inviteUser(1L, chat.id(), 2L))
                .isInstanceOf(ApiException.class);
    }

    // --- leaveChat ---

    @Test
    void leaveChat() throws ApiException {
        var chat = createGroup(1L, 2L);

        chatService.leaveChat(2L, chat.id());
        flushAndClear();

        var updated = chatService.getChat(1L, chat.id());
        assertThat(updated.members()).hasSize(1);
    }

    @Test
    void leaveChat_savedChat_throws() throws ApiException {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.SAVED, null, null));
        flushAndClear();

        assertThatThrownBy(() -> chatService.leaveChat(1L, chat.id()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void leaveChat_owner_throws() throws ApiException {
        var chat = createGroup(1L, 2L);

        assertThatThrownBy(() -> chatService.leaveChat(1L, chat.id()))
                .isInstanceOf(ApiException.class);
    }

    // --- kickUser ---

    @Test
    void kickUser() throws ApiException {
        var chat = createGroup(1L, 2L, 3L);

        chatService.kickUser(1L, chat.id(), 2L);
        flushAndClear();

        var updated = chatService.getChat(1L, chat.id());
        assertThat(updated.members()).hasSize(2);
    }

    @Test
    void kickUser_notGroup_throws() throws ApiException {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.DIRECT, null, List.of(2L)));
        flushAndClear();

        assertThatThrownBy(() -> chatService.kickUser(1L, chat.id(), 2L))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void kickUser_notOwner_throws() throws ApiException {
        var chat = createGroup(1L, 2L, 3L);

        assertThatThrownBy(() -> chatService.kickUser(2L, chat.id(), 3L))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void kickUser_self_throws() throws ApiException {
        var chat = createGroup(1L, 2L);

        assertThatThrownBy(() -> chatService.kickUser(1L, chat.id(), 1L))
                .isInstanceOf(ApiException.class);
    }

    // --- deleteChat ---

    @Test
    void deleteChat() throws ApiException {
        var chat = createGroup(1L, 2L);

        chatService.deleteChat(1L, chat.id());
        flushAndClear();

        assertThatThrownBy(() -> chatService.getChat(1L, chat.id()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void deleteChat_notOwner_throws() throws ApiException {
        var chat = createGroup(1L, 2L);

        assertThatThrownBy(() -> chatService.deleteChat(2L, chat.id()))
                .isInstanceOf(ApiException.class);
    }

    // --- getMessages ---

    @Test
    void getMessages_pagination() throws ApiException {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.DIRECT, null, List.of(2L)));
        flushAndClear();

        for (int i = 0; i < 5; i++) {
            messageService.sendMessage(1L, chat.id(), new SendMessageRequest("message " + i));
            flushAndClear();
        }

        var messagesSort = Sort.by(Sort.Direction.DESC, "createdAt", "id");

        var all = chatService.getMessages(1L, chat.id(), PageRequest.of(0, 50, messagesSort));
        assertThat(all.getContent()).hasSize(5);
        assertThat(all.getTotalElements()).isEqualTo(5);

        var secondPage = chatService.getMessages(1L, chat.id(), PageRequest.of(1, 3, messagesSort));
        assertThat(secondPage.getContent()).hasSize(2);
        assertThat(secondPage.getTotalElements()).isEqualTo(5);
    }

    @Test
    void getMessages_notAccessible_throws() throws ApiException {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.SAVED, null, null));
        flushAndClear();

        var pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        assertThatThrownBy(() -> chatService.getMessages(999L, chat.id(), pageable))
                .isInstanceOf(ApiException.class);
    }

    // --- helpers ---

    private ChatDto createGroup(long ownerId, Long... memberIds) throws ApiException {
        var chat = chatService.createChat(ownerId, new CreateChatRequest(ChatType.GROUP, "Test Group", List.of(memberIds)));
        flushAndClear();
        return chat;
    }

}
