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
class ChatMemberServiceTest {

    private static final UUID USER_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER_3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID UNKNOWN_USER = UUID.fromString("00000000-0000-0000-0000-000000000999");

    @Autowired ChatService chatService;
    @Autowired ChatMemberService chatMemberService;
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

    // --- getMembers ---

    @Test
    void getMembers_returnsMembersPage() throws ApiException {
        var chat = createGroup(USER_1, USER_2, USER_3);

        var page = chatMemberService.getMembers(chat.id(), PageRequest.of(0, 50, Sort.by("joinedAt")));

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void getMembers_notMember_throws() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        runAs(UNKNOWN_USER);
        var pageable = PageRequest.of(0, 50);
        assertThatThrownBy(() -> chatMemberService.getMembers(chat.id(), pageable))
                .isInstanceOf(ApiException.class);
    }

    // --- getMember ---

    @Test
    void getMember_returnsMember() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        var member = chatMemberService.getMember(chat.id(), USER_2);

        assertThat(member.profileId()).isEqualTo(USER_2);
        assertThat(member.name()).isEqualTo("User 00000000");
        assertThat(member.photoUrl()).isEqualTo("https://example.com/" + USER_2 + ".png");
        assertThat(member.role()).isEqualTo(ChatMemberRole.MEMBER);
    }

    @Test
    void getMember_notMember_throws() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        assertThatThrownBy(() -> chatMemberService.getMember(chat.id(), UNKNOWN_USER))
                .isInstanceOf(ApiException.class);
    }

    // --- inviteUser ---

    @Test
    void inviteUser() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        var member = chatMemberService.inviteUser(chat.id(), USER_3);

        assertThat(member.profileId()).isEqualTo(USER_3);
        assertThat(member.name()).isEqualTo("User 00000000");
        assertThat(member.photoUrl()).isEqualTo("https://example.com/" + USER_3 + ".png");
        assertThat(member.role()).isEqualTo(ChatMemberRole.MEMBER);
        flushAndClear();

        var updated = chatService.getChat(chat.id());
        assertThat(updated.members()).hasSize(3);
    }

    @Test
    void inviteUser_notGroup_throws() throws ApiException {
        runAs(USER_1);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.DIRECT, null, List.of(USER_2)));
        flushAndClear();

        assertThatThrownBy(() -> chatMemberService.inviteUser(chat.id(), USER_3))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void inviteUser_notOwner_throws() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        runAs(USER_2);
        assertThatThrownBy(() -> chatMemberService.inviteUser(chat.id(), USER_3))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void inviteUser_alreadyMember_throws() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        assertThatThrownBy(() -> chatMemberService.inviteUser(chat.id(), USER_2))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void inviteUser_whenRelationNotAccepted_throws() throws ApiException {
        var chat = createGroup(USER_1, USER_2);
        profileClient.setStatus(USER_3, RelationStatus.BLOCKED);

        assertThatThrownBy(() -> chatMemberService.inviteUser(chat.id(), USER_3))
                .isInstanceOf(ApiException.class);
    }

    // --- leaveChat ---

    @Test
    void leaveChat() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        runAs(USER_2);
        chatMemberService.leaveChat(chat.id());
        flushAndClear();

        runAs(USER_1);
        var updated = chatService.getChat(chat.id());
        assertThat(updated.members()).hasSize(1);
    }

    @Test
    void leaveChat_savedChat_throws() throws ApiException {
        runAs(USER_1);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.SAVED, null, null));
        flushAndClear();

        assertThatThrownBy(() -> chatMemberService.leaveChat(chat.id()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void leaveChat_owner_throws() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        assertThatThrownBy(() -> chatMemberService.leaveChat(chat.id()))
                .isInstanceOf(ApiException.class);
    }

    // --- kickUser ---

    @Test
    void kickUser() throws ApiException {
        var chat = createGroup(USER_1, USER_2, USER_3);

        chatMemberService.kickUser(chat.id(), USER_2);
        flushAndClear();

        var updated = chatService.getChat(chat.id());
        assertThat(updated.members()).hasSize(2);
    }

    @Test
    void kickUser_notGroup_throws() throws ApiException {
        runAs(USER_1);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.DIRECT, null, List.of(USER_2)));
        flushAndClear();

        assertThatThrownBy(() -> chatMemberService.kickUser(chat.id(), USER_2))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void kickUser_notOwner_throws() throws ApiException {
        var chat = createGroup(USER_1, USER_2, USER_3);

        runAs(USER_2);
        assertThatThrownBy(() -> chatMemberService.kickUser(chat.id(), USER_3))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void kickUser_self_throws() throws ApiException {
        var chat = createGroup(USER_1, USER_2);

        assertThatThrownBy(() -> chatMemberService.kickUser(chat.id(), USER_1))
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
