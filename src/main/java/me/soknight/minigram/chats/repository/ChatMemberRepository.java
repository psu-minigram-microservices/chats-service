package me.soknight.minigram.chats.repository;

import me.soknight.minigram.chats.model.entity.ChatMemberEntity;
import me.soknight.minigram.chats.model.entity.ChatMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatMemberRepository extends JpaRepository<ChatMemberEntity, ChatMemberId> {

    @Query("select cm.id.userId from ChatMemberEntity cm where cm.chat.id = :chatId")
    List<Long> findUserIdsByChatId(long chatId);

    @Query("""
            select chat_member
            from ChatMemberEntity chat_member
            where chat_member.chat.id = :chatId and chat_member.id.userId = :userId
            """)
    Optional<ChatMemberEntity> findById(long chatId, long userId);

    @Query("""
            select count(chat_member) > 0
            from ChatMemberEntity chat_member
            where chat_member.chat.id = :chatId and chat_member.id.userId = :userId
            """)
    boolean existsById(long chatId, long userId);

}
