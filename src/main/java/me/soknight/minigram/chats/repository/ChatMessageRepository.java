package me.soknight.minigram.chats.repository;

import me.soknight.minigram.chats.model.entity.ChatMessageEntity;
import me.soknight.minigram.chats.model.entity.ChatMessageId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, ChatMessageId> {

    @EntityGraph(attributePaths = {"chat", "sender"})
    @Query("""
            select chat_message
            from ChatMessageEntity chat_message
            where chat_message.id.chatId = :chatId
            """)
    Page<ChatMessageEntity> findByChatId(long chatId, Pageable pageable);

    @Query("""
            select chat_message
            from ChatMessageEntity chat_message
            where chat_message.chat.id = :chatId and chat_message.id.messageId = :messageId
            """)
    Optional<ChatMessageEntity> findById(long chatId, long messageId);

    @Query("""
            select chat_message.id.messageId from ChatMessageEntity chat_message
            where chat_message.id.chatId = :chatId and chat_message.id.messageId <> :excludedMessageId
            order by chat_message.createdAt desc, chat_message.id.messageId desc
            limit 1
            """)
    Optional<Long> findLastMessageIdByChatIdExcluding(long chatId, long excludedMessageId);

}
