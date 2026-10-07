package org.sopt.buddys.domain.chat.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.sopt.buddys.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            select case when count(m) > 0 then true else false end
            from ChatMessage m
            where m.id = :messageId
              and m.chatRoom.id = :chatRoomId
              and (
                :partnerMessagesHiddenAfter is null
                or m.sender.id <> :partnerId
                or m.createdAt <= :partnerMessagesHiddenAfter
              )
            """)
    boolean existsVisibleMessage(
            @Param("messageId") Long messageId,
            @Param("chatRoomId") Long chatRoomId,
            @Param("partnerId") Long partnerId,
            @Param("partnerMessagesHiddenAfter") LocalDateTime partnerMessagesHiddenAfter
    );

    @Query("""
            select m
            from ChatMessage m
            join fetch m.sender
            where m.chatRoom.id = :chatRoomId
              and (
                :partnerMessagesHiddenAfter is null
                or m.sender.id <> :partnerId
                or m.createdAt <= :partnerMessagesHiddenAfter
              )
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
            @Param("partnerId") Long partnerId,
            @Param("partnerMessagesHiddenAfter") LocalDateTime partnerMessagesHiddenAfter,
            Pageable pageable
    );
}
