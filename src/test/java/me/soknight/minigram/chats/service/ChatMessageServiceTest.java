package me.soknight.minigram.chats.service;

import jakarta.persistence.EntityManager;
import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.model.dto.ChatDto;
import me.soknight.minigram.chats.model.request.CreateChatRequest;
import me.soknight.minigram.chats.model.request.EditMessageRequest;
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
class ChatMessageServiceTest {

    @Autowired ChatMessageService messageService;
    @Autowired ChatService chatService;
    @Autowired EntityManager em;

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    private ChatDto createDirectChat() throws ApiException {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.DIRECT, null, List.of(2L)));
        flushAndClear();
        return chat;
    }

    // --- sendMessage ---

    @Test
    void sendMessage() throws ApiException {
        var chat = createDirectChat();

        var message = messageService.sendMessage(1L, chat.id(), new SendMessageRequest("Hello!"));

        assertThat(message.content()).isEqualTo("Hello!");
        assertThat(message.sender().userId()).isEqualTo(1L);
        assertThat(message.chat().id()).isEqualTo(chat.id());
        flushAndClear();

        var updatedChat = chatService.getChat(1L, chat.id());
        assertThat(updatedChat.lastMessageId()).isEqualTo(message.id());
    }

    @Test
    void sendMessage_updatesLastMessageId() throws ApiException {
        var chat = createDirectChat();

        var msg1 = messageService.sendMessage(1L, chat.id(), new SendMessageRequest("first"));
        flushAndClear();
        var msg2 = messageService.sendMessage(1L, chat.id(), new SendMessageRequest("second"));
        flushAndClear();

        var updatedChat = chatService.getChat(1L, chat.id());
        assertThat(updatedChat.lastMessageId()).isEqualTo(msg2.id());
    }

    @Test
    void sendMessage_notMember_throws() throws ApiException {
        var chat = createDirectChat();

        assertThatThrownBy(() -> messageService.sendMessage(999L, chat.id(), new SendMessageRequest("Hello!")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void sendMessage_trimContent() throws ApiException {
        var chat = createDirectChat();

        var message = messageService.sendMessage(1L, chat.id(), new SendMessageRequest("  hello  "));

        assertThat(message.content()).isEqualTo("hello");
    }

    // --- editMessage ---

    @Test
    void editMessage() throws ApiException {
        var chat = createDirectChat();
        var message = messageService.sendMessage(1L, chat.id(), new SendMessageRequest("original"));
        flushAndClear();

        var edited = messageService.editMessage(1L, chat.id(), message.id(), new EditMessageRequest("updated"));

        assertThat(edited.content()).isEqualTo("updated");
        assertThat(edited.id()).isEqualTo(message.id());
    }

    @Test
    void editMessage_notAuthor_throws() throws ApiException {
        var chat = createDirectChat();
        var message = messageService.sendMessage(1L, chat.id(), new SendMessageRequest("original"));
        flushAndClear();

        assertThatThrownBy(() -> messageService.editMessage(2L, chat.id(), message.id(), new EditMessageRequest("updated")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void editMessage_notFound_throws() throws ApiException {
        var chat = createDirectChat();

        assertThatThrownBy(() -> messageService.editMessage(1L, chat.id(), 999L, new EditMessageRequest("updated")))
                .isInstanceOf(ApiException.class);
    }

    // --- deleteMessage ---

    @Test
    void deleteMessage() throws ApiException {
        var chat = createDirectChat();
        var message = messageService.sendMessage(1L, chat.id(), new SendMessageRequest("to delete"));
        flushAndClear();

        messageService.deleteMessage(1L, chat.id(), message.id());
        flushAndClear();

        var messages = messageService.getMessages(1L, chat.id(), PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        assertThat(messages.getContent()).isEmpty();
    }

    @Test
    void deleteMessage_updatesLastMessageIdToPrevious() throws ApiException {
        var chat = createDirectChat();
        var msg1 = messageService.sendMessage(1L, chat.id(), new SendMessageRequest("first"));
        flushAndClear();
        var msg2 = messageService.sendMessage(1L, chat.id(), new SendMessageRequest("second"));
        flushAndClear();

        messageService.deleteMessage(1L, chat.id(), msg2.id());
        flushAndClear();

        var updatedChat = chatService.getChat(1L, chat.id());
        assertThat(updatedChat.lastMessageId()).isEqualTo(msg1.id());
    }

    @Test
    void deleteMessage_lastOne_clearsLastMessageId() throws ApiException {
        var chat = createDirectChat();
        var message = messageService.sendMessage(1L, chat.id(), new SendMessageRequest("only message"));
        flushAndClear();

        messageService.deleteMessage(1L, chat.id(), message.id());
        flushAndClear();

        var updatedChat = chatService.getChat(1L, chat.id());
        assertThat(updatedChat.lastMessageId()).isNull();
    }

    @Test
    void deleteMessage_notAuthor_throws() throws ApiException {
        var chat = createDirectChat();
        var message = messageService.sendMessage(1L, chat.id(), new SendMessageRequest("original"));
        flushAndClear();

        assertThatThrownBy(() -> messageService.deleteMessage(2L, chat.id(), message.id()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void deleteMessage_notFound_throws() throws ApiException {
        var chat = createDirectChat();

        assertThatThrownBy(() -> messageService.deleteMessage(1L, chat.id(), 999L))
                .isInstanceOf(ApiException.class);
    }

}
