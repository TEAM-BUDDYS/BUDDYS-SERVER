package org.sopt.buddys.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.chat.code.ChatErrorCode;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.repository.ChatRoomMemberRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomRepository;
import org.sopt.buddys.domain.chat.service.result.ChatRoomResult;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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

  @BeforeEach
  void setUp() {
    cleanUp();
  }

  @AfterEach
  void tearDown() {
    cleanUp();
  }

  @DisplayName("내가 참여한 채팅방을 조회하면 채팅방과 상대방 정보를 반환한다")
  @Test
  void getChatRoom_joinedChatRoom_returnsChatRoomDetail() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    User participant = userRepository.save(
        createUser("participant@test.com", "provider-participant", "상대방")
    );
    ChatRoom chatRoom = chatRoomService.createOrGetChatRoom(
        user.getId(),
        participant.getId()
    ).chatRoom();

    // when
    ChatRoomResult result = chatRoomService.getChatRoom(user.getId(), chatRoom.getId());

    // then
    assertThat(result.chatRoom().getId()).isEqualTo(chatRoom.getId());
    assertThat(result.participant().getId()).isEqualTo(participant.getId());
    assertThat(result.participant().getNickname()).isEqualTo(participant.getNickname());
  }

  @DisplayName("존재하지 않는 채팅방을 조회하면 CHAT-E002 예외가 발생한다")
  @Test
  void getChatRoom_notFoundChatRoom_throwsChatRoomNotFound() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Long notFoundChatRoomId = 1L;

    // when, then
    assertThatThrownBy(() -> chatRoomService.getChatRoom(user.getId(), notFoundChatRoomId))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND)
        );
  }

  @DisplayName("존재하지만 내가 참여하지 않은 채팅방을 조회하면 GLB-E003 예외가 발생한다")
  @Test
  void getChatRoom_notJoinedChatRoom_throwsForbidden() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    User otherUser = userRepository.save(
        createUser("other@test.com", "provider-other", "다른사용자")
    );
    User participant = userRepository.save(
        createUser("participant@test.com", "provider-participant", "상대방")
    );
    ChatRoom chatRoom = chatRoomService.createOrGetChatRoom(
        otherUser.getId(),
        participant.getId()
    ).chatRoom();

    // when, then
    assertThatThrownBy(() -> chatRoomService.getChatRoom(user.getId(), chatRoom.getId()))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(GlobalErrorCode.FORBIDDEN)
        );
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

  private void cleanUp() {
    chatRoomMemberRepository.deleteAllInBatch();
    chatRoomRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
  }
}
