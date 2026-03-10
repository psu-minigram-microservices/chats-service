package me.soknight.minigram.chats.service;

import jakarta.persistence.EntityManager;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.attribute.ChatMemberRole;
import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.model.dto.ChatDto;
import me.soknight.minigram.chats.model.request.CreateChatRequest;
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
class ChatMemberServiceTest {

    @Autowired ChatService chatService;
    @Autowired ChatMemberService chatMemberService;
    @Autowired EntityManager em;

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    // --- getMembers ---

    @Test
    void getMembers_returnsMembersPage() throws ApiException {
        var chat = createGroup(1L, 2L, 3L);

        var page = chatMemberService.getMembers(1L, chat.id(), PageRequest.of(0, 50, Sort.by("joinedAt")));

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void getMembers_notMember_throws() throws ApiException {
        var chat = createGroup(1L, 2L);

        var pageable = PageRequest.of(0, 50);
        assertThatThrownBy(() -> chatMemberService.getMembers(999L, chat.id(), pageable))
                .isInstanceOf(ApiException.class);
    }

    // --- getMember ---

    @Test
    void getMember_returnsMember() throws ApiException {
        var chat = createGroup(1L, 2L);

        var member = chatMemberService.getMember(1L, chat.id(), 2L);

        assertThat(member.userId()).isEqualTo(2L);
        assertThat(member.role()).isEqualTo(ChatMemberRole.MEMBER);
    }

    @Test
    void getMember_notMember_throws() throws ApiException {
        var chat = createGroup(1L, 2L);

        assertThatThrownBy(() -> chatMemberService.getMember(1L, chat.id(), 999L))
                .isInstanceOf(ApiException.class);
    }

    // --- inviteUser ---

    @Test
    void inviteUser() throws ApiException {
        var chat = createGroup(1L, 2L);

        var member = chatMemberService.inviteUser(1L, chat.id(), 3L);

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

        assertThatThrownBy(() -> chatMemberService.inviteUser(1L, chat.id(), 3L))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void inviteUser_notOwner_throws() throws ApiException {
        var chat = createGroup(1L, 2L);

        assertThatThrownBy(() -> chatMemberService.inviteUser(2L, chat.id(), 3L))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void inviteUser_alreadyMember_throws() throws ApiException {
        var chat = createGroup(1L, 2L);

        assertThatThrownBy(() -> chatMemberService.inviteUser(1L, chat.id(), 2L))
                .isInstanceOf(ApiException.class);
    }

    // --- leaveChat ---

    @Test
    void leaveChat() throws ApiException {
        var chat = createGroup(1L, 2L);

        chatMemberService.leaveChat(2L, chat.id());
        flushAndClear();

        var updated = chatService.getChat(1L, chat.id());
        assertThat(updated.members()).hasSize(1);
    }

    @Test
    void leaveChat_savedChat_throws() throws ApiException {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.SAVED, null, null));
        flushAndClear();

        assertThatThrownBy(() -> chatMemberService.leaveChat(1L, chat.id()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void leaveChat_owner_throws() throws ApiException {
        var chat = createGroup(1L, 2L);

        assertThatThrownBy(() -> chatMemberService.leaveChat(1L, chat.id()))
                .isInstanceOf(ApiException.class);
    }

    // --- kickUser ---

    @Test
    void kickUser() throws ApiException {
        var chat = createGroup(1L, 2L, 3L);

        chatMemberService.kickUser(1L, chat.id(), 2L);
        flushAndClear();

        var updated = chatService.getChat(1L, chat.id());
        assertThat(updated.members()).hasSize(2);
    }

    @Test
    void kickUser_notGroup_throws() throws ApiException {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.DIRECT, null, List.of(2L)));
        flushAndClear();

        assertThatThrownBy(() -> chatMemberService.kickUser(1L, chat.id(), 2L))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void kickUser_notOwner_throws() throws ApiException {
        var chat = createGroup(1L, 2L, 3L);

        assertThatThrownBy(() -> chatMemberService.kickUser(2L, chat.id(), 3L))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void kickUser_self_throws() throws ApiException {
        var chat = createGroup(1L, 2L);

        assertThatThrownBy(() -> chatMemberService.kickUser(1L, chat.id(), 1L))
                .isInstanceOf(ApiException.class);
    }

    // --- helpers ---

    private ChatDto createGroup(long ownerId, Long... memberIds) throws ApiException {
        var chat = chatService.createChat(ownerId, new CreateChatRequest(ChatType.GROUP, "Test Group", List.of(memberIds)));
        flushAndClear();
        return chat;
    }

}
