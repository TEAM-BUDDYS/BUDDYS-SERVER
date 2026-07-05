package org.sopt.buddys.domain.chat.repository;

import java.util.Optional;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.entity.ChatRoomMember;
import org.sopt.buddys.domain.chat.entity.ChatRoomMemberId;
import org.sopt.buddys.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, ChatRoomMemberId> {

  @Query("""
      select cr as chatRoom,
             participant as participant
      from ChatRoomMember myMember
      join myMember.chatRoom cr
      join ChatRoomMember participantMember
        on participantMember.chatRoom = cr
       and participantMember.user.id <> :userId
      join participantMember.user participant
      where cr.id = :chatRoomId
        and myMember.user.id = :userId
      """)
  Optional<ChatRoomDetailProjection> findChatRoomDetailByIdAndUserId(
      @Param("chatRoomId") Long chatRoomId,
      @Param("userId") Long userId
  );

  interface ChatRoomDetailProjection {

    ChatRoom getChatRoom();

    User getParticipant();
  }
}
