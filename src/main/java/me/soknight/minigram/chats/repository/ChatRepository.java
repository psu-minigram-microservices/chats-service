package me.soknight.minigram.chats.repository;

import me.soknight.minigram.chats.model.entity.ChatEntity;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ChatRepository extends JpaRepository<ChatEntity, Long> {

    @EntityGraph(attributePaths = "members")
    @Query(value = """
            select distinct chat
            from ChatEntity chat
            join chat.members membership
            where membership.id.userId = :userId
            """,
            countQuery = """
            select count(distinct chat.id)
            from ChatEntity chat
            join chat.members membership
            where membership.id.userId = :userId
            """)
    Page<ChatEntity> findAllByMemberUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = "members")
    @Query("""
            select distinct chat
            from ChatEntity chat
            join chat.members membership
            where chat.id = :chatId and membership.id.userId = :userId
            """)
    Optional<ChatEntity> findAccessibleById(long chatId, UUID userId);

    @Modifying
    @Query("""
            update ChatEntity chat
            set chat.lastMessageId = :messageId, chat.updatedAt = :now
            where chat.id = :chatId
            """)
    void updateLastMessageId(long chatId, @Nullable Long messageId, Instant now);

    @Modifying
    @Query("""
            update ChatEntity chat
            set chat.updatedAt = :now
            where chat.id = :chatId
            """)
    void touch(long chatId, Instant now);

    @Modifying
    @Query("""
            update ChatEntity chat
            set chat.messageSequence = chat.messageSequence + 1
            where chat.id = :chatId
            """)
    void incrementMessageSequence(long chatId);

    @Query("""
            select chat.messageSequence
            from ChatEntity chat
            where chat.id = :chatId
            """)
    long getMessageSequence(long chatId);

}
