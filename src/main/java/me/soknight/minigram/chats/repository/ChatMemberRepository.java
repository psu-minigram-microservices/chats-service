package me.soknight.minigram.chats.repository;

import me.soknight.minigram.chats.model.entity.ChatMemberEntity;
import me.soknight.minigram.chats.model.entity.ChatMemberId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMemberRepository extends JpaRepository<ChatMemberEntity, ChatMemberId> {

    @Query("""
            select chat_member.id.profileId
            from ChatMemberEntity chat_member
            where chat_member.chat.id = :chatId
            """)
    List<UUID> findUserIdsByChatId(long chatId);

    @Query("""
            select chat_member
            from ChatMemberEntity chat_member
            where chat_member.chat.id = :chatId
            """)
    Page<ChatMemberEntity> findByChatId(long chatId, Pageable pageable);

    @Query("""
            select chat_member
            from ChatMemberEntity chat_member
            where chat_member.chat.id = :chatId and chat_member.id.profileId = :userId
            """)
    Optional<ChatMemberEntity> findById(long chatId, UUID userId);

    @Query("""
            select count(chat_member) > 0
            from ChatMemberEntity chat_member
            where chat_member.chat.id = :chatId and chat_member.id.profileId = :userId
            """)
    boolean existsById(long chatId, UUID userId);

}
