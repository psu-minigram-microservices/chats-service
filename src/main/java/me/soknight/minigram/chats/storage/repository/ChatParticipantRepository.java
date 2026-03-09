package me.soknight.minigram.chats.storage.repository;

import me.soknight.minigram.chats.storage.model.ChatParticipantEntity;
import me.soknight.minigram.chats.storage.model.ChatParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipantEntity, ChatParticipantId> {

    @Query("""
            select participant
            from ChatParticipantEntity participant
            where participant.chat.id = :chatId and participant.id.userId = :userId
            """)
    Optional<ChatParticipantEntity> findByChat_IdAndUserId(long chatId, long userId);

    @Query("""
            select count(participant) > 0
            from ChatParticipantEntity participant
            where participant.chat.id = :chatId and participant.id.userId = :userId
            """)
    boolean existsByChat_IdAndUserId(long chatId, long userId);

}
