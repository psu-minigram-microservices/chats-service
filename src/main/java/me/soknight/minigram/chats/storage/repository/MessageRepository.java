package me.soknight.minigram.chats.storage.repository;

import me.soknight.minigram.chats.storage.model.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    List<MessageEntity> findByChat_IdOrderByCreatedAtDescIdDesc(long chatId);

    Optional<MessageEntity> findTopByChat_IdAndIdNotOrderByCreatedAtDescIdDesc(long chatId, long excludedMessageId);

}
