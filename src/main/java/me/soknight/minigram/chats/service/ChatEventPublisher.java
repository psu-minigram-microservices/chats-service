package me.soknight.minigram.chats.service;

import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.model.websocket.ChatEvent;
import me.soknight.minigram.chats.repository.ChatMemberRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ChatEventPublisher {

    private static final String USER_EVENTS_DESTINATION = "/queue/events";

    private final @NonNull SimpMessagingTemplate messagingTemplate;
    private final @NonNull ChatMemberRepository chatMemberRepository;

    public void publish(long chatId, @NonNull ChatEvent event) {
        var memberUserIds = chatMemberRepository.findUserIdsByChatId(chatId);
        for (long userId : memberUserIds) {
            messagingTemplate.convertAndSendToUser(
                    Long.toString(userId),
                    USER_EVENTS_DESTINATION,
                    event
            );
        }
    }

}
