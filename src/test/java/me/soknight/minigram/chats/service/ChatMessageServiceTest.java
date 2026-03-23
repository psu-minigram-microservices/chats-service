package me.soknight.minigram.chats.service;

import jakarta.persistence.EntityManager;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.model.dto.ChatDto;
import me.soknight.minigram.chats.model.request.CreateChatRequest;
import me.soknight.minigram.chats.model.request.EditMessageRequest;
import me.soknight.minigram.chats.model.request.SendMessageRequest;
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
class ChatMessageServiceTest {

    private static final UUID USER_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID UNKNOWN_USER = UUID.fromString("00000000-0000-0000-0000-000000000999");

    @Autowired ChatMessageService messageService;
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

    private ChatDto createDirectChat() throws ApiException {
        runAs(USER_1);
        var chat = chatService.createChat(new CreateChatRequest(ChatType.DIRECT, null, List.of(USER_2)));
        flushAndClear();
        return chat;
    }

    // --- sendMessage ---

    @Test
    void sendMessage() throws ApiException {
        var chat = createDirectChat();

        var message = messageService.sendMessage(chat.id(), new SendMessageRequest("Hello!"));

        assertThat(message.content()).isEqualTo("Hello!");
        assertThat(message.sender().profileId()).isEqualTo(USER_1);
        assertThat(message.sender().name()).isEqualTo("User 00000000");
        assertThat(message.sender().photoUrl()).isEqualTo("https://example.com/" + USER_1 + ".png");
        assertThat(message.chat().id()).isEqualTo(chat.id());
        flushAndClear();

        var updatedChat = chatService.getChat(chat.id());
        assertThat(updatedChat.lastMessageId()).isEqualTo(message.id());
    }

    @Test
    void sendMessage_updatesLastMessageId() throws ApiException {
        var chat = createDirectChat();

        var msg1 = messageService.sendMessage(chat.id(), new SendMessageRequest("first"));
        flushAndClear();
        var msg2 = messageService.sendMessage(chat.id(), new SendMessageRequest("second"));
        flushAndClear();

        var updatedChat = chatService.getChat(chat.id());
        assertThat(updatedChat.lastMessageId()).isEqualTo(msg2.id());
    }

    @Test
    void sendMessage_notMember_throws() throws ApiException {
        var chat = createDirectChat();

        runAs(UNKNOWN_USER);
        assertThatThrownBy(() -> messageService.sendMessage(chat.id(), new SendMessageRequest("Hello!")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void sendMessage_trimContent() throws ApiException {
        var chat = createDirectChat();

        var message = messageService.sendMessage(chat.id(), new SendMessageRequest("  hello  "));

        assertThat(message.content()).isEqualTo("hello");
    }

    @Test
    void sendMessage_whenDirectRelationNotAccepted_throws() throws ApiException {
        var chat = createDirectChat();
        profileClient.setStatus(USER_2, RelationStatus.BLOCKED);

        assertThatThrownBy(() -> messageService.sendMessage(chat.id(), new SendMessageRequest("Hello!")))
                .isInstanceOf(ApiException.class);
    }

    // --- editMessage ---

    @Test
    void editMessage() throws ApiException {
        var chat = createDirectChat();
        var message = messageService.sendMessage(chat.id(), new SendMessageRequest("original"));
        flushAndClear();

        var edited = messageService.editMessage(chat.id(), message.id(), new EditMessageRequest("updated"));

        assertThat(edited.content()).isEqualTo("updated");
        assertThat(edited.id()).isEqualTo(message.id());
    }

    @Test
    void editMessage_notAuthor_throws() throws ApiException {
        var chat = createDirectChat();
        var message = messageService.sendMessage(chat.id(), new SendMessageRequest("original"));
        flushAndClear();

        runAs(USER_2);
        assertThatThrownBy(() -> messageService.editMessage(chat.id(), message.id(), new EditMessageRequest("updated")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void editMessage_notFound_throws() throws ApiException {
        var chat = createDirectChat();

        assertThatThrownBy(() -> messageService.editMessage(chat.id(), 999L, new EditMessageRequest("updated")))
                .isInstanceOf(ApiException.class);
    }

    // --- deleteMessage ---

    @Test
    void deleteMessage() throws ApiException {
        var chat = createDirectChat();
        var message = messageService.sendMessage(chat.id(), new SendMessageRequest("to delete"));
        flushAndClear();

        messageService.deleteMessage(chat.id(), message.id());
        flushAndClear();

        var messages = messageService.getMessages(chat.id(), PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        assertThat(messages.getContent()).isEmpty();
    }

    @Test
    void deleteMessage_updatesLastMessageIdToPrevious() throws ApiException {
        var chat = createDirectChat();
        var msg1 = messageService.sendMessage(chat.id(), new SendMessageRequest("first"));
        flushAndClear();
        var msg2 = messageService.sendMessage(chat.id(), new SendMessageRequest("second"));
        flushAndClear();

        messageService.deleteMessage(chat.id(), msg2.id());
        flushAndClear();

        var updatedChat = chatService.getChat(chat.id());
        assertThat(updatedChat.lastMessageId()).isEqualTo(msg1.id());
    }

    @Test
    void deleteMessage_lastOne_clearsLastMessageId() throws ApiException {
        var chat = createDirectChat();
        var message = messageService.sendMessage(chat.id(), new SendMessageRequest("only message"));
        flushAndClear();

        messageService.deleteMessage(chat.id(), message.id());
        flushAndClear();

        var updatedChat = chatService.getChat(chat.id());
        assertThat(updatedChat.lastMessageId()).isNull();
    }

    @Test
    void deleteMessage_notAuthor_throws() throws ApiException {
        var chat = createDirectChat();
        var message = messageService.sendMessage(chat.id(), new SendMessageRequest("original"));
        flushAndClear();

        runAs(USER_2);
        assertThatThrownBy(() -> messageService.deleteMessage(chat.id(), message.id()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void deleteMessage_notFound_throws() throws ApiException {
        var chat = createDirectChat();

        assertThatThrownBy(() -> messageService.deleteMessage(chat.id(), 999L))
                .isInstanceOf(ApiException.class);
    }

}
