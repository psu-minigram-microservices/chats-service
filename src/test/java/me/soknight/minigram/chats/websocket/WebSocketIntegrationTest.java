package me.soknight.minigram.chats.websocket;

import io.jsonwebtoken.Jwts;
import me.soknight.minigram.chats.model.attribute.ChatType;
import me.soknight.minigram.chats.model.request.CreateChatRequest;
import me.soknight.minigram.chats.model.request.EditMessageRequest;
import me.soknight.minigram.chats.model.request.SendMessageRequest;
import me.soknight.minigram.chats.service.ChatService;
import me.soknight.minigram.chats.service.MessageService;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTest {

    @LocalServerPort int port;
    @Value("${server.jwt.secret}") String jwtSecret;

    @Autowired ChatService chatService;
    @Autowired MessageService messageService;

    private WebSocketStompClient stompClient;
    private StompSession session;
    private BlockingQueue<Map<String, Object>> events;

    @BeforeEach
    void setUp() throws Exception {
        events = new LinkedBlockingQueue<>();

        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());

        session = connectAsUser(1L);
    }

    @AfterEach
    void tearDown() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    @Test
    void sendMessage_receivesMessageSentEvent() throws Exception {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.DIRECT, null, List.of(2L)));
        subscribeToEvents();

        messageService.sendMessage(1L, chat.id(), new SendMessageRequest("hello"));

        var event = pollEvent();
        assertThat(event).isNotNull();
        assertThat(event.get("type")).isEqualTo("MESSAGE_SENT");
        assertThat(((Number) event.get("chat_id")).longValue()).isEqualTo(chat.id());
    }

    @Test
    void editMessage_receivesMessageEditedEvent() throws Exception {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.DIRECT, null, List.of(2L)));
        var message = messageService.sendMessage(1L, chat.id(), new SendMessageRequest("original"));
        subscribeToEvents();

        messageService.editMessage(1L, message.id(), new EditMessageRequest("updated"));

        var event = pollEvent();
        assertThat(event).isNotNull();
        assertThat(event.get("type")).isEqualTo("MESSAGE_EDITED");
    }

    @Test
    void deleteMessage_receivesMessageDeletedEvent() throws Exception {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.DIRECT, null, List.of(2L)));
        var message = messageService.sendMessage(1L, chat.id(), new SendMessageRequest("to delete"));
        subscribeToEvents();

        messageService.deleteMessage(1L, message.id());

        var event = pollEvent();
        assertThat(event).isNotNull();
        assertThat(event.get("type")).isEqualTo("MESSAGE_DELETED");
        assertThat(((Number) event.get("chat_id")).longValue()).isEqualTo(chat.id());
    }

    @Test
    void inviteUser_receivesMemberJoinedEvent() throws Exception {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.GROUP, "Test", List.of(2L)));
        subscribeToEvents();

        chatService.inviteUser(1L, chat.id(), 3L);

        var event = pollEvent();
        assertThat(event).isNotNull();
        assertThat(event.get("type")).isEqualTo("MEMBER_JOINED");
        assertThat(((Number) event.get("chat_id")).longValue()).isEqualTo(chat.id());
    }

    @Test
    void leaveChat_receivesMemberLeftEvent() throws Exception {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.GROUP, "Test", List.of(2L)));
        subscribeToEvents();

        chatService.leaveChat(2L, chat.id());

        var event = pollEvent();
        assertThat(event).isNotNull();
        assertThat(event.get("type")).isEqualTo("MEMBER_LEFT");
    }

    @Test
    void kickUser_receivesMemberLeftEvent() throws Exception {
        var chat = chatService.createChat(1L, new CreateChatRequest(ChatType.GROUP, "Test", List.of(2L, 3L)));
        subscribeToEvents();

        chatService.kickUser(1L, chat.id(), 2L);

        var event = pollEvent();
        assertThat(event).isNotNull();
        assertThat(event.get("type")).isEqualTo("MEMBER_LEFT");
    }

    @Test
    void nonMember_doesNotReceiveEvent() throws Exception {
        // user 1 is connected, but not a member of this chat
        var chat = chatService.createChat(2L, new CreateChatRequest(ChatType.DIRECT, null, List.of(3L)));
        subscribeToEvents();

        messageService.sendMessage(2L, chat.id(), new SendMessageRequest("secret"));

        var event = events.poll(2, TimeUnit.SECONDS);
        assertThat(event).isNull();
    }

    @Test
    void invalidToken_connectionRejected() {
        var url = "ws://localhost:" + port + "/ws?token=invalid-jwt-token";

        var future = stompClient.connectAsync(url, new WebSocketHttpHeaders(), new StompSessionHandlerAdapter() {});

        assertThat(future).failsWithin(5, TimeUnit.SECONDS);
    }

    // --- helpers ---

    private StompSession connectAsUser(long userId) throws Exception {
        var token = generateJwt(userId);
        var url = "ws://localhost:" + port + "/ws?token=" + token;

        return stompClient.connectAsync(url, new WebSocketHttpHeaders(), new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
    }

    private void subscribeToEvents() throws Exception {
        session.subscribe("/user/queue/events", new StompFrameHandler() {
            @Override
            public @NonNull Type getPayloadType(@NonNull StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                events.add((Map<String, Object>) payload);
            }
        });
        // give the subscription time to register
        Thread.sleep(200);
    }

    private Map<String, Object> pollEvent() throws InterruptedException {
        return events.poll(5, TimeUnit.SECONDS);
    }

    private String generateJwt(long userId) {
        var keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        var key = new SecretKeySpec(keyBytes, "HmacSHA256");

        return Jwts.builder()
                .subject(Long.toString(userId))
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

}
