package org.sopt.buddys.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.chat.code.ChatErrorCode;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.repository.ChatRoomMemberRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomRepository;
import org.sopt.buddys.domain.chat.repository.ChatUserBlockRepository;
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
class ChatUserBlockServiceTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private ChatUserBlockService chatUserBlockService;

  @Autowired
  private ChatRoomService chatRoomService;

  @Autowired
  private ChatUserBlockRepository chatUserBlockRepository;

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

  @DisplayName("채팅방 상대방을 차단하면 차단 관계가 저장된다")
  @Test
  void blockChatPartner_savesBlockRelationship() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    User partner = userRepository.save(createUser("partner@test.com", "provider-partner", "상대방"));
    ChatRoom chatRoom = chatRoomService.createOrGetChatRoom(user.getId(), partner.getId()).chatRoom();

    // when
    chatUserBlockService.blockChatPartner(user.getId(), chatRoom.getId());

    // then
    assertThat(chatUserBlockRepository.existsBlockBetween(user.getId(), partner.getId())).isTrue();
  }

  @DisplayName("이미 차단한 상대방을 다시 차단해도 예외 없이 멱등하게 처리된다")
  @Test
  void blockChatPartner_repeatedCalls_areIdempotent() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    User partner = userRepository.save(createUser("partner@test.com", "provider-partner", "상대방"));
    ChatRoom chatRoom = chatRoomService.createOrGetChatRoom(user.getId(), partner.getId()).chatRoom();

    // when
    assertThatCode(() -> {
      chatUserBlockService.blockChatPartner(user.getId(), chatRoom.getId());
      chatUserBlockService.blockChatPartner(user.getId(), chatRoom.getId());
    }).doesNotThrowAnyException();

    // then
    assertThat(chatUserBlockRepository.count()).isOne();
  }

  @DisplayName("존재하지 않는 채팅방을 차단하려 하면 CHAT_ROOM_NOT_FOUND 예외가 발생한다")
  @Test
  void blockChatPartner_notFoundChatRoom_throwsChatRoomNotFound() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));

    // when, then
    assertThatThrownBy(() -> chatUserBlockService.blockChatPartner(user.getId(), 999_999L))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
  }

  @DisplayName("채팅방 멤버가 아닌 사용자가 차단을 시도하면 FORBIDDEN 예외가 발생한다")
  @Test
  void blockChatPartner_notMember_throwsForbidden() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    User partner = userRepository.save(createUser("partner@test.com", "provider-partner", "상대방"));
    User outsider = userRepository.save(createUser("outsider@test.com", "provider-outsider", "제3자"));
    ChatRoom chatRoom = chatRoomService.createOrGetChatRoom(user.getId(), partner.getId()).chatRoom();

    // when, then
    assertThatThrownBy(() -> chatUserBlockService.blockChatPartner(outsider.getId(), chatRoom.getId()))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(GlobalErrorCode.FORBIDDEN);
  }

  private User createUser(String email, String providerId, String nickname) {
    return User.builder()
        .email(email)
        .provider(AuthProvider.KAKAO)
        .providerId(providerId)
        .nickname(nickname)
        .build();
  }

  private void cleanUp() {
    chatUserBlockRepository.deleteAllInBatch();
    chatRoomMemberRepository.deleteAllInBatch();
    chatRoomRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
  }
}
