package org.sopt.buddys.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.chat.code.ChatErrorCode;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.repository.ChatMessageRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomMemberRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomRepository;
import org.sopt.buddys.domain.chat.repository.ChatUserBlockRepository;
import org.sopt.buddys.domain.chat.service.result.ChatMessageSendResult;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
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
class ChatMessageCommandServiceTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private ChatMessageCommandService chatMessageCommandService;

  @Autowired
  private ChatUserBlockService chatUserBlockService;

  @Autowired
  private ChatRoomService chatRoomService;

  @Autowired
  private ChatMessageRepository chatMessageRepository;

  @Autowired
  private ChatUserBlockRepository chatUserBlockRepository;

  @Autowired
  private ChatRoomMemberRepository chatRoomMemberRepository;

  @Autowired
  private ChatRoomRepository chatRoomRepository;

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

  @DisplayName("차단 관계가 없으면 정상적으로 메시지를 보낼 수 있다")
  @Test
  void sendMessage_noBlock_succeeds() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    User partner = userRepository.save(createUser("partner@test.com", "provider-partner", "상대방"));
    ChatRoom chatRoom = chatRoomService.createOrGetChatRoom(user.getId(), partner.getId()).chatRoom();

    // when
    ChatMessageSendResult result = chatMessageCommandService.sendMessage(
        user.getId(), chatRoom.getId(), "안녕하세요"
    );

    // then
    assertThat(result.message().getMessage()).isEqualTo("안녕하세요");
  }

  @DisplayName("내가 상대방을 차단했으면 그 상대방과의 채팅방에 메시지를 보낼 수 없다")
  @Test
  void sendMessage_blockedByMe_throwsBlockedChatPartner() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    User partner = userRepository.save(createUser("partner@test.com", "provider-partner", "상대방"));
    ChatRoom chatRoom = chatRoomService.createOrGetChatRoom(user.getId(), partner.getId()).chatRoom();
    chatUserBlockService.blockChatPartner(user.getId(), chatRoom.getId());

    // when, then
    assertThatThrownBy(() ->
        chatMessageCommandService.sendMessage(user.getId(), chatRoom.getId(), "안녕하세요")
    )
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ChatErrorCode.BLOCKED_CHAT_PARTNER);
  }

  @DisplayName("상대방이 나를 차단했으면 그 상대방에게 메시지를 보낼 수 없다")
  @Test
  void sendMessage_blockedByPartner_throwsBlockedChatPartner() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    User partner = userRepository.save(createUser("partner@test.com", "provider-partner", "상대방"));
    ChatRoom chatRoom = chatRoomService.createOrGetChatRoom(user.getId(), partner.getId()).chatRoom();
    chatUserBlockService.blockChatPartner(partner.getId(), chatRoom.getId());

    // when, then
    assertThatThrownBy(() ->
        chatMessageCommandService.sendMessage(user.getId(), chatRoom.getId(), "안녕하세요")
    )
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ChatErrorCode.BLOCKED_CHAT_PARTNER);
  }

  @DisplayName("기존 채팅 내역은 차단 이후에도 그대로 유지된다")
  @Test
  void sendMessage_afterBlock_existingHistoryIsPreserved() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    User partner = userRepository.save(createUser("partner@test.com", "provider-partner", "상대방"));
    ChatRoom chatRoom = chatRoomService.createOrGetChatRoom(user.getId(), partner.getId()).chatRoom();
    chatMessageCommandService.sendMessage(user.getId(), chatRoom.getId(), "차단 전 메시지");

    // when
    chatUserBlockService.blockChatPartner(user.getId(), chatRoom.getId());

    // then
    assertThat(chatMessageRepository.count()).isOne();
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
    chatMessageRepository.deleteAllInBatch();
    chatRoomMemberRepository.deleteAllInBatch();
    chatRoomRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
  }
}
