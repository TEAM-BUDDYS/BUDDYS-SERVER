package org.sopt.buddys.domain.chat.repository;

import java.time.LocalDateTime;
import org.sopt.buddys.domain.chat.entity.ChatRoomMember;
import org.sopt.buddys.domain.chat.entity.ChatRoomMemberId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, ChatRoomMemberId> {

  @Query("""
      select cr.id as chatRoomId,
             participant.id as participantUserId,
             participant.nickname as participantNickname,
             participant.profileImageUrl as participantProfileImageUrl,
             lastMessage.message as lastMessage,
             lastMessage.createdAt as lastMessageSentAt,
             (
               select count(unreadMessage)
               from ChatMessage unreadMessage
               where unreadMessage.chatRoom = cr
                 and unreadMessage.sender.id <> :userId
                 and (
                   myMember.lastReadMessageId is null
                   or unreadMessage.id > myMember.lastReadMessageId
                 )
             ) as unreadMessageCount
      from ChatRoomMember myMember
      join myMember.chatRoom cr
      join ChatRoomMember participantMember
        on participantMember.chatRoom = cr
       and participantMember.user.id <> :userId
      join participantMember.user participant
      left join ChatMessage lastMessage
        on lastMessage.chatRoom = cr
       and not exists (
         select 1
         from ChatMessage newerMessage
         where newerMessage.chatRoom = cr
           and (
             newerMessage.createdAt > lastMessage.createdAt
             or (
               newerMessage.createdAt = lastMessage.createdAt
               and newerMessage.id > lastMessage.id
             )
           )
       )
      where myMember.user.id = :userId
      order by coalesce(lastMessage.createdAt, cr.createdAt) desc,
               case when lastMessage.id is null then 0 else lastMessage.id end desc,
               cr.id desc
      """)
  Slice<ChatRoomListProjection> findChatRoomListByUserId(
      @Param("userId") Long userId,
      Pageable pageable
  );

  interface ChatRoomListProjection {

    Long getChatRoomId();

    Long getParticipantUserId();

    String getParticipantNickname();

    String getParticipantProfileImageUrl();

    String getLastMessage();

    LocalDateTime getLastMessageSentAt();

    long getUnreadMessageCount();
  }
}
