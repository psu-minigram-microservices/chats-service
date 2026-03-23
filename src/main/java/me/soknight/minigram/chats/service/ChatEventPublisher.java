package me.soknight.minigram.chats.service;

import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.model.websocket.ChatEvent;
import me.soknight.minigram.chats.repository.ChatMemberRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ChatEventPublisher {

    private static final String USER_EVENTS_DESTINATION = "/queue/events";

    private final @NonNull SimpMessagingTemplate messagingTemplate;
    private final @NonNull ChatMemberRepository chatMemberRepository;

    public void publish(long chatId, @NonNull ChatEvent event) {
        var memberUserIds = chatMemberRepository.findUserIdsByChatId(chatId);
        publishToUsers(memberUserIds, event);
    }

    public void publishToUsers(@NonNull Iterable<UUID> userIds, @NonNull ChatEvent event) {
        var recipients = List.copyOf((java.util.Collection<UUID>) userIds);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendToUsers(recipients, event);
                }
            });
        } else {
            sendToUsers(recipients, event);
        }
    }

    private void sendToUsers(@NonNull Iterable<UUID> userIds, @NonNull ChatEvent event) {
        for (UUID userId : userIds) {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    USER_EVENTS_DESTINATION,
                    event
            );
        }
    }

}
