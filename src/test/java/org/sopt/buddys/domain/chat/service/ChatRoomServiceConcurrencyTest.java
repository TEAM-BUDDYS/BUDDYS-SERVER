package org.sopt.buddys.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.chat.repository.ChatRoomMemberRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomRepository;
import org.sopt.buddys.domain.chat.util.DirectChatKey;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
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
class ChatRoomServiceConcurrencyTest {

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

  @DisplayName("동시에 같은 두 유저의 채팅방을 생성해도 1개의 채팅방만 생성된다")
  @Test
  void createOrGetChatRoom_concurrentRequest_createsOnlyOneChatRoom() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    User participant = userRepository.save(createUser("participant@test.com", "provider-participant", "상대방"));
    String directChatKey = DirectChatKey.from(user.getId(), participant.getId());

    ExecutorService executorService = Executors.newFixedThreadPool(2);
    CountDownLatch readyLatch = new CountDownLatch(2);
    CountDownLatch startLatch = new CountDownLatch(1);

    try {
      Future<Long> firstResult = executorService.submit(
          () -> createChatRoomAtSameTime(user.getId(), participant.getId(), readyLatch, startLatch)
      );
      Future<Long> secondResult = executorService.submit(
          () -> createChatRoomAtSameTime(user.getId(), participant.getId(), readyLatch, startLatch)
      );

      assertThat(readyLatch.await(3, TimeUnit.SECONDS)).isTrue();

      // when
      startLatch.countDown();
      Long firstChatRoomId = firstResult.get(5, TimeUnit.SECONDS);
      Long secondChatRoomId = secondResult.get(5, TimeUnit.SECONDS);

      // then
      assertThat(firstChatRoomId).isEqualTo(secondChatRoomId);
      assertThat(chatRoomRepository.count()).isEqualTo(1);
      assertThat(chatRoomMemberRepository.count()).isEqualTo(2);
      assertThat(chatRoomRepository.findByDirectChatKey(directChatKey))
          .isPresent()
          .get()
          .extracting(chatRoom -> chatRoom.getId())
          .isEqualTo(firstChatRoomId);
    } finally {
      executorService.shutdownNow();
    }
  }

  private Long createChatRoomAtSameTime(
      Long userId,
      Long participantUserId,
      CountDownLatch readyLatch,
      CountDownLatch startLatch
  ) throws InterruptedException {

    readyLatch.countDown();
    assertThat(startLatch.await(3, TimeUnit.SECONDS)).isTrue();
    return chatRoomService.createOrGetChatRoom(userId, participantUserId)
        .chatRoom()
        .getId();
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
