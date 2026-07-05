package org.sopt.buddys.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.repository.ChatRoomMemberRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomRepository;
import org.sopt.buddys.domain.chat.service.result.ChatRoomListResult;
import org.sopt.buddys.domain.chat.service.result.ChatRoomListResult.ChatRoomListItemResult;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ChatRoomServiceTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private ChatRoomService chatRoomService;

  @Autowired
  private ChatRoomRepository chatRoomRepository;

  @Autowired
  private ChatRoomMemberRepository chatRoomMemberRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    cleanUp();
  }

  @AfterEach
  void tearDown() {
    cleanUp();
  }

  @DisplayName("채팅방 목록은 마지막 메시지가 최신인 순서로 정렬된다")
  @Test
  void getChatRooms_ordersByLatestMessageSentAtDesc() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    User oldParticipant = userRepository.save(
        createUser("old@test.com", "provider-old", "오래된상대")
    );
    User latestParticipant = userRepository.save(
        createUser("latest@test.com", "provider-latest", "최신상대")
    );
    User noMessageParticipant = userRepository.save(
        createUser("no-message@test.com", "provider-no-message", "메시지없는상대")
    );

    ChatRoom oldChatRoom = chatRoomService.createOrGetChatRoom(
        user.getId(),
        oldParticipant.getId()
    ).chatRoom();
    ChatRoom latestChatRoom = chatRoomService.createOrGetChatRoom(
        user.getId(),
        latestParticipant.getId()
    ).chatRoom();
    ChatRoom noMessageChatRoom = chatRoomService.createOrGetChatRoom(
        user.getId(),
        noMessageParticipant.getId()
    ).chatRoom();
    updateChatRoomCreatedAt(
        noMessageChatRoom.getId(),
        LocalDateTime.of(2026, 7, 5, 12, 0)
    );

    insertMessage(
        oldChatRoom.getId(),
        oldParticipant.getId(),
        "먼저 온 메시지",
        LocalDateTime.of(2026, 7, 5, 10, 0)
    );
    insertMessage(
        latestChatRoom.getId(),
        latestParticipant.getId(),
        "가장 최근 메시지",
        LocalDateTime.of(2026, 7, 5, 11, 0)
    );
    insertMessage(
        latestChatRoom.getId(),
        latestParticipant.getId(),
        "나중에 저장됐지만 더 과거인 메시지",
        LocalDateTime.of(2026, 7, 5, 9, 0)
    );

    // when
    ChatRoomListResult result = chatRoomService.getChatRooms(user.getId(), 0, 20);

    // then
    assertThat(result.chatRooms())
        .extracting(ChatRoomListItemResult::chatRoomId)
        .containsExactly(noMessageChatRoom.getId(), latestChatRoom.getId(), oldChatRoom.getId());
    assertThat(result.chatRooms().get(0).lastMessage()).isNull();
    assertThat(result.chatRooms().get(1).lastMessage()).isEqualTo("가장 최근 메시지");
  }

  @DisplayName("읽지 않은 메시지 수는 마지막으로 읽은 메시지 이후 상대방이 보낸 메시지만 계산한다")
  @Test
  void getChatRooms_countsUnreadMessagesSentByParticipantAfterLastReadMessageId() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    User participant = userRepository.save(
        createUser("participant@test.com", "provider-participant", "상대방")
    );
    ChatRoom chatRoom = chatRoomService.createOrGetChatRoom(
        user.getId(),
        participant.getId()
    ).chatRoom();
    Long lastReadMessageId = insertMessage(
        chatRoom.getId(),
        participant.getId(),
        "이미 읽은 상대방 메시지",
        LocalDateTime.of(2026, 7, 5, 10, 0)
    );
    updateLastReadMessageId(chatRoom.getId(), user.getId(), lastReadMessageId);
    insertMessage(
        chatRoom.getId(),
        participant.getId(),
        "읽지 않은 상대방 메시지",
        LocalDateTime.of(2026, 7, 5, 11, 0)
    );
    insertMessage(
        chatRoom.getId(),
        user.getId(),
        "내가 보낸 메시지",
        LocalDateTime.of(2026, 7, 5, 11, 30)
    );

    // when
    ChatRoomListResult result = chatRoomService.getChatRooms(user.getId(), 0, 20);

    // then
    assertThat(result.chatRooms()).hasSize(1);
    assertThat(result.chatRooms().get(0).unreadMessageCount()).isEqualTo(1);
  }

  @DisplayName("마지막 읽음 메시지는 같은 채팅방의 메시지만 저장할 수 있다")
  @Test
  void updateLastReadMessageId_withMessageInOtherChatRoom_fails() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    User firstParticipant = userRepository.save(
        createUser("first@test.com", "provider-first", "첫번째상대")
    );
    User secondParticipant = userRepository.save(
        createUser("second@test.com", "provider-second", "두번째상대")
    );
    ChatRoom firstChatRoom = chatRoomService.createOrGetChatRoom(
        user.getId(),
        firstParticipant.getId()
    ).chatRoom();
    ChatRoom secondChatRoom = chatRoomService.createOrGetChatRoom(
        user.getId(),
        secondParticipant.getId()
    ).chatRoom();
    Long otherChatRoomMessageId = insertMessage(
        secondChatRoom.getId(),
        secondParticipant.getId(),
        "다른 채팅방 메시지",
        LocalDateTime.of(2026, 7, 5, 12, 0)
    );

    // when, then
    assertThatThrownBy(() ->
        updateLastReadMessageId(firstChatRoom.getId(), user.getId(), otherChatRoomMessageId)
    ).isInstanceOf(DataIntegrityViolationException.class);
  }

  private User createUser(
      String email,
      String providerId,
      String nickname
  ) {

    return User.builder()
        .email(email)
        .provider(AuthProvider.KAKAO)
        .providerId(providerId)
        .nickname(nickname)
        .build();
  }

  private Long insertMessage(
      Long chatRoomId,
      Long senderId,
      String message,
      LocalDateTime createdAt
  ) {

    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
          var preparedStatement = connection.prepareStatement(
              """
                  INSERT INTO chat_message (chat_room_id, sender_id, message, created_at)
                  VALUES (?, ?, ?, ?)
                  """,
              java.sql.Statement.RETURN_GENERATED_KEYS
          );
          preparedStatement.setLong(1, chatRoomId);
          preparedStatement.setLong(2, senderId);
          preparedStatement.setString(3, message);
          preparedStatement.setObject(4, createdAt);
          return preparedStatement;
        },
        keyHolder
    );

    return keyHolder.getKey().longValue();
  }

  private void updateLastReadMessageId(
      Long chatRoomId,
      Long userId,
      Long lastReadMessageId
  ) {

    jdbcTemplate.update(
        """
            UPDATE chat_room_member
            SET last_read_message_id = ?
            WHERE chat_room_id = ?
              AND user_id = ?
            """,
        lastReadMessageId,
        chatRoomId,
        userId
    );
  }

  private void updateChatRoomCreatedAt(
      Long chatRoomId,
      LocalDateTime createdAt
  ) {

    jdbcTemplate.update(
        """
            UPDATE chat_room
            SET created_at = ?
            WHERE id = ?
            """,
        createdAt,
        chatRoomId
    );
  }

  private void cleanUp() {
    jdbcTemplate.update("UPDATE chat_room_member SET last_read_message_id = NULL");
    jdbcTemplate.update("DELETE FROM chat_message");
    chatRoomMemberRepository.deleteAllInBatch();
    chatRoomRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
  }
}
