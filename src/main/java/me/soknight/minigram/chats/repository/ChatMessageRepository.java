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
    @Query("select message from ChatMessageEntity message where message.id.chatId = :chatId")
    Page<ChatMessageEntity> findByChatId(long chatId, Pageable pageable);

    @Query("""
            select message.id.messageId from ChatMessageEntity message
            where message.id.chatId = :chatId and message.id.messageId <> :excludedMessageId
            order by message.createdAt desc, message.id.messageId desc
            limit 1
            """)
    Optional<Long> findLastMessageIdByChatIdExcluding(long chatId, long excludedMessageId);

}
