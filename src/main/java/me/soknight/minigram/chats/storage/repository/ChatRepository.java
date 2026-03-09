package me.soknight.minigram.chats.storage.repository;

import me.soknight.minigram.chats.storage.model.ChatEntity;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<ChatEntity, Long> {

    @EntityGraph(attributePaths = "members")
    @Query("""
            select distinct chat
            from ChatEntity chat
            join chat.members membership
            where membership.id.userId = :userId
            order by chat.updatedAt desc, chat.id desc
            """)
    List<ChatEntity> findAllByMemberUserId(long userId);

    @EntityGraph(attributePaths = "members")
    @Query("""
            select distinct chat
            from ChatEntity chat
            join chat.members membership
            where chat.id = :chatId and membership.id.userId = :userId
            """)
    Optional<ChatEntity> findAccessibleById(long chatId, long userId);

    @Modifying
    @Query("update ChatEntity chat set chat.lastMessageId = :messageId, chat.updatedAt = CURRENT_TIMESTAMP where chat.id = :chatId")
    void updateLastMessageId(long chatId, @Nullable Long messageId);

    @Modifying
    @Query("update ChatEntity chat set chat.updatedAt = CURRENT_TIMESTAMP where chat.id = :chatId")
    void touch(long chatId);

}
