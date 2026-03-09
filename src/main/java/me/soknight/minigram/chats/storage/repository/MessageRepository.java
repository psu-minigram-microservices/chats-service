package me.soknight.minigram.chats.storage.repository;

import me.soknight.minigram.chats.storage.model.MessageEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    @EntityGraph(attributePaths = {"chat", "sender"})
    @Query("""
            select message from MessageEntity message
            where message.chat.id = :chatId
            order by message.createdAt desc, message.id desc
            offset :offset rows
            fetch first :limit rows only
            """)
    List<MessageEntity> findByChatId(long chatId, int offset, int limit);

    @Query("""
            select message.id from MessageEntity message
            where message.chat.id = :chatId and message.id <> :excludedMessageId
            order by message.createdAt desc, message.id desc
            limit 1
            """)
    Optional<Long> findLastIdByChatIdExcluding(long chatId, long excludedMessageId);

}
