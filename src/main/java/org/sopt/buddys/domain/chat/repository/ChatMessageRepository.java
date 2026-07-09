package org.sopt.buddys.domain.chat.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.sopt.buddys.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    boolean existsByIdAndChatRoomId(Long id, Long chatRoomId);

    @Query("""
            select m
            from ChatMessage m
            join fetch m.sender
            where m.chatRoom.id = :chatRoomId
              and (
                :cursorSentAt is null
                or m.createdAt < :cursorSentAt
                or (
                  m.createdAt = :cursorSentAt
                  and m.id < :cursorMessageId
                )
              )
            order by m.createdAt desc, m.id desc
            """)
    List<ChatMessage> findMessagesByChatRoomId(
            @Param("chatRoomId") Long chatRoomId,
            @Param("cursorSentAt") LocalDateTime cursorSentAt,
            @Param("cursorMessageId") Long cursorMessageId,
            Pageable pageable
    );
}
