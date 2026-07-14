package org.sopt.buddys.domain.chat.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.sopt.buddys.domain.chat.entity.QChatMessage;
import org.sopt.buddys.domain.chat.entity.QChatRoom;
import org.sopt.buddys.domain.chat.entity.QChatRoomMember;
import org.sopt.buddys.domain.chat.repository.ChatRoomMemberRepository.ChatRoomListProjection;
import org.sopt.buddys.domain.user.entity.QUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

public class ChatRoomMemberRepositoryImpl implements ChatRoomMemberRepositoryCustom {

  private static final QChatRoomMember myMember = new QChatRoomMember("myMember");
  private static final QChatRoomMember participantMember = new QChatRoomMember("participantMember");
  private static final QChatRoom chatRoom = QChatRoom.chatRoom;
  private static final QUser participant = new QUser("participant");
  private static final QChatMessage lastMessage = new QChatMessage("lastMessage");
  private static final QChatMessage newerMessage = new QChatMessage("newerMessage");
  private static final QChatMessage unreadMessage = new QChatMessage("unreadMessage");

  private final JPAQueryFactory queryFactory;

  public ChatRoomMemberRepositoryImpl(EntityManager entityManager) {
    this.queryFactory = new JPAQueryFactory(entityManager);
  }

  @Override
  public Slice<ChatRoomListProjection> findChatRoomListByUserId(
      Long userId,
      Pageable pageable
  ) {

    Expression<Long> unreadMessageCount = unreadMessageCount(userId);
    List<ChatRoomListProjection> chatRooms = baseChatRoomListQuery(userId, unreadMessageCount)
        .orderBy(
            latestActivityAt().desc(),
            Expressions.numberTemplate(
                Long.class,
                "case when {0} is null then 0 else {0} end",
                lastMessage.id
            ).desc(),
            chatRoom.id.desc()
        )
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize() + 1L)
        .fetch()
        .stream()
        .map(tuple -> toProjection(tuple, unreadMessageCount))
        .toList();

    boolean hasNext = chatRooms.size() > pageable.getPageSize();
    if (hasNext) {
      chatRooms = chatRooms.subList(0, pageable.getPageSize());
    }

    return new SliceImpl<>(chatRooms, pageable, hasNext);
  }

  @Override
  public Optional<ChatRoomListProjection> findChatRoomListItemByUserIdAndChatRoomId(
      Long userId,
      Long chatRoomId
  ) {

    Expression<Long> unreadMessageCount = unreadMessageCount(userId);
    return baseChatRoomListQuery(userId, unreadMessageCount)
        .where(chatRoom.id.eq(chatRoomId))
        .fetch()
        .stream()
        .findFirst()
        .map(tuple -> toProjection(tuple, unreadMessageCount));
  }

  private JPAQuery<Tuple> baseChatRoomListQuery(
      Long userId,
      Expression<Long> unreadMessageCount
  ) {

    return queryFactory
        .select(
            chatRoom.id,
            participant.id,
            participant.nickname,
            participant.profileImageUrl,
            lastMessage.message,
            lastMessage.createdAt,
            unreadMessageCount
        )
        .from(myMember)
        .join(myMember.chatRoom, chatRoom)
        .join(participantMember)
        .on(
            participantMember.chatRoom.eq(chatRoom),
            participantMember.user.id.ne(userId)
        )
        .join(participantMember.user, participant)
        .leftJoin(lastMessage)
        .on(
            lastMessage.chatRoom.eq(chatRoom),
            JPAExpressions
                .selectOne()
                .from(newerMessage)
                .where(
                    newerMessage.chatRoom.eq(chatRoom),
                    newerMessage.createdAt.gt(lastMessage.createdAt)
                        .or(
                            newerMessage.createdAt.eq(lastMessage.createdAt)
                                .and(newerMessage.id.gt(lastMessage.id))
                        )
                )
                .notExists()
        )
        .where(myMember.user.id.eq(userId));
  }

  private Expression<Long> unreadMessageCount(Long userId) {
    return JPAExpressions
        .select(unreadMessage.count())
        .from(unreadMessage)
        .where(
            unreadMessage.chatRoom.eq(chatRoom),
            unreadMessage.sender.id.ne(userId),
            myMember.lastReadMessageId.isNull()
                .or(unreadMessage.id.gt(myMember.lastReadMessageId))
        );
  }

  private DateTimeExpression<LocalDateTime> latestActivityAt() {
    return Expressions.dateTimeTemplate(
        LocalDateTime.class,
        "coalesce({0}, {1})",
        lastMessage.createdAt,
        chatRoom.createdAt
    );
  }

  private ChatRoomListProjection toProjection(
      Tuple tuple,
      Expression<Long> unreadMessageCount
  ) {

    return new ChatRoomListProjectionDto(
        tuple.get(chatRoom.id),
        tuple.get(participant.id),
        tuple.get(participant.nickname),
        tuple.get(participant.profileImageUrl),
        tuple.get(lastMessage.message),
        tuple.get(lastMessage.createdAt),
        Optional.ofNullable(tuple.get(unreadMessageCount)).orElse(0L)
    );
  }

  private record ChatRoomListProjectionDto(
      Long chatRoomId,
      Long participantUserId,
      String participantNickname,
      String participantProfileImageUrl,
      String lastMessage,
      LocalDateTime lastMessageSentAt,
      long unreadMessageCount
  ) implements ChatRoomListProjection {

    @Override
    public Long getChatRoomId() {
      return chatRoomId;
    }

    @Override
    public Long getParticipantUserId() {
      return participantUserId;
    }

    @Override
    public String getParticipantNickname() {
      return participantNickname;
    }

    @Override
    public String getParticipantProfileImageUrl() {
      return participantProfileImageUrl;
    }

    @Override
    public String getLastMessage() {
      return lastMessage;
    }

    @Override
    public LocalDateTime getLastMessageSentAt() {
      return lastMessageSentAt;
    }

    @Override
    public long getUnreadMessageCount() {
      return unreadMessageCount;
    }
  }
}
