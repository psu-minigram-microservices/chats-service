package me.soknight.minigram.chats.service;

import lombok.AllArgsConstructor;
import me.soknight.minigram.chats.repository.ChatMemberRepository;
import me.soknight.minigram.chats.repository.ChatMessageRepository;
import me.soknight.minigram.chats.repository.ChatRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ChatMemberService {

    private final @NonNull ChatRepository chatRepository;
    private final @NonNull ChatMemberRepository chatMemberRepository;
    private final @NonNull ChatMessageRepository chatMessageRepository;
    private final @NonNull ChatEventPublisher eventPublisher;

}
