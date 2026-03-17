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
class ChatMemberServiceTest {

    private static final UUID USER_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER_3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID UNKNOWN_USER = UUID.fromString("00000000-0000-0000-0000-000000000999");

    @Autowired ChatService chatService;
    @Autowired ChatMemberService chatMemberService;
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

    // --- getMembers ---

    @Test
    void getMembers_returnsMembersPage() throws ApiException {
        var chat = createGroup(USER_1, USER_2, USER_3);

        var page = chatMemberService.getMembers(USER_1, chat.id(), PageRequest.of(0, 50, Sort.by("joinedAt")));

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void getMembers_notMember_throws() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        var pageable = PageRequest.of(0, 50);
        assertThatThrownBy(() -> chatMemberService.getMembers(UNKNOWN_USER, chat.id(), pageable))
                .isInstanceOf(ApiException.class);
    }

    // --- getMember ---

    @Test
    void getMember_returnsMember() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        var member = chatMemberService.getMember(USER_1, chat.id(), USER_2);

        assertThat(member.userId()).isEqualTo(USER_2);
        assertThat(member.role()).isEqualTo(ChatMemberRole.MEMBER);
    }

    @Test
    void getMember_notMember_throws() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        assertThatThrownBy(() -> chatMemberService.getMember(USER_1, chat.id(), UNKNOWN_USER))
                .isInstanceOf(ApiException.class);
    }

    // --- inviteUser ---

    @Test
    void inviteUser() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        var member = chatMemberService.inviteUser(USER_1, chat.id(), USER_3);

        assertThat(member.userId()).isEqualTo(USER_3);
        assertThat(member.role()).isEqualTo(ChatMemberRole.MEMBER);
        flushAndClear();

        var updated = chatService.getChat(USER_1, chat.id());
        assertThat(updated.members()).hasSize(3);
    }

    @Test
    void inviteUser_notGroup_throws() throws ApiException {
        var chat = chatService.createChat(USER_1, new CreateChatRequest(ChatType.DIRECT, null, List.of(USER_2)));
        flushAndClear();

        assertThatThrownBy(() -> chatMemberService.inviteUser(USER_1, chat.id(), USER_3))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void inviteUser_notOwner_throws() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        assertThatThrownBy(() -> chatMemberService.inviteUser(USER_2, chat.id(), USER_3))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void inviteUser_alreadyMember_throws() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        assertThatThrownBy(() -> chatMemberService.inviteUser(USER_1, chat.id(), USER_2))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void inviteUser_whenRelationNotAccepted_throws() throws ApiException {
        var chat = createGroup(USER_1, USER_2);
        profileRelationsClient.setStatus(USER_3, RelationStatus.BLOCKED);

        assertThatThrownBy(() -> chatMemberService.inviteUser(USER_1, chat.id(), USER_3))
                .isInstanceOf(ApiException.class);
    }

    // --- leaveChat ---

    @Test
    void leaveChat() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        chatMemberService.leaveChat(USER_2, chat.id());
        flushAndClear();

        var updated = chatService.getChat(USER_1, chat.id());
        assertThat(updated.members()).hasSize(1);
    }

    @Test
    void leaveChat_savedChat_throws() throws ApiException {
        var chat = chatService.createChat(USER_1, new CreateChatRequest(ChatType.SAVED, null, null));
        flushAndClear();

        assertThatThrownBy(() -> chatMemberService.leaveChat(USER_1, chat.id()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void leaveChat_owner_throws() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        assertThatThrownBy(() -> chatMemberService.leaveChat(USER_1, chat.id()))
                .isInstanceOf(ApiException.class);
    }

    // --- kickUser ---

    @Test
    void kickUser() throws ApiException {
        var chat = createGroup(USER_1, USER_2, USER_3);

        chatMemberService.kickUser(USER_1, chat.id(), USER_2);
        flushAndClear();

        var updated = chatService.getChat(USER_1, chat.id());
        assertThat(updated.members()).hasSize(2);
    }

    @Test
    void kickUser_notGroup_throws() throws ApiException {
        var chat = chatService.createChat(USER_1, new CreateChatRequest(ChatType.DIRECT, null, List.of(USER_2)));
        flushAndClear();

        assertThatThrownBy(() -> chatMemberService.kickUser(USER_1, chat.id(), USER_2))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void kickUser_notOwner_throws() throws ApiException {
        var chat = createGroup(USER_1, USER_2, USER_3);

        assertThatThrownBy(() -> chatMemberService.kickUser(USER_2, chat.id(), USER_3))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void kickUser_self_throws() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        assertThatThrownBy(() -> chatMemberService.kickUser(USER_1, chat.id(), USER_1))
                .isInstanceOf(ApiException.class);
    }

    // --- helpers ---

    private ChatDto createGroup(UUID ownerId, UUID... memberIds) throws ApiException {
        var chat = chatService.createChat(ownerId, new CreateChatRequest(ChatType.GROUP, "Test Group", List.of(memberIds)));
        flushAndClear();
        return chat;
    }

}
