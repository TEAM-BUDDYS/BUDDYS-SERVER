package org.sopt.buddys.domain.chat.repository;

import java.util.Optional;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.entity.ChatRoomMember;
import org.sopt.buddys.domain.chat.entity.ChatRoomMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, ChatRoomMemberId> {

  @Query("""
      select m1.chatRoom
      from ChatRoomMember m1
      join ChatRoomMember m2 on m2.chatRoom = m1.chatRoom
      where m1.user.id = :userId
        and m2.user.id = :participantUserId
        and (
          select count(m3)
          from ChatRoomMember m3
          where m3.chatRoom = m1.chatRoom
        ) = 2
      """)
  Optional<ChatRoom> findDirectChatRoom(
      @Param("userId") Long userId,
      @Param("participantUserId") Long participantUserId
  );
}
