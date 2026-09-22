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
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.repository.ChatMessageRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomMemberRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomRepository;
import org.sopt.buddys.domain.chat.repository.ChatUserBlockRepository;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ChatMessageBlockConcurrencyTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private ChatUserBlockService chatUserBlockService;

  @Autowired
  private ChatRoomService chatRoomService;

  @Autowired
  private ChatRoomRepository chatRoomRepository;

  @Autowired
  private ChatRoomMemberRepository chatRoomMemberRepository;

  @Autowired
  private ChatUserBlockRepository chatUserBlockRepository;

  @Autowired
  private ChatMessageRepository chatMessageRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @BeforeEach
  void setUp() {
    cleanUp();
  }

  @AfterEach
  void tearDown() {
    cleanUp();
  }

  @DisplayName("메시지 전송이 채팅방 행을 잠근 동안에는 같은 채팅방의 차단 처리가 대기했다가 잠금 해제 후 진행된다")
  @Test
  void blockChatPartner_waitsForSendMessageChatRoomLockToBeReleased() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    User partner = userRepository.save(createUser("partner@test.com", "provider-partner", "상대방"));
    ChatRoom chatRoom = chatRoomService.createOrGetChatRoom(user.getId(), partner.getId()).chatRoom();

    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    CountDownLatch lockAcquiredLatch = new CountDownLatch(1);
    CountDownLatch releaseLockLatch = new CountDownLatch(1);
    ExecutorService executorService = Executors.newFixedThreadPool(2);

    try {
      // sendMessage가 하는 것과 동일하게 채팅방 행에 PESSIMISTIC_WRITE 락을 건 채
      // 커밋하지 않고 붙잡고 있는 트랜잭션을 흉내낸다.
      Future<?> lockHolder = executorService.submit(() -> {
        transactionTemplate.executeWithoutResult(status -> {
          chatRoomRepository.findByIdForUpdate(chatRoom.getId());
          lockAcquiredLatch.countDown();
          awaitQuietly(releaseLockLatch);
        });
        return null;
      });

      assertThat(lockAcquiredLatch.await(3, TimeUnit.SECONDS)).isTrue();

      Future<?> blocker = executorService.submit(() -> {
        chatUserBlockService.blockChatPartner(partner.getId(), chatRoom.getId());
        return null;
      });

      // when: 락 보유 중에는 차단 트랜잭션이 완료되지 못하고 대기해야 한다
      Thread.sleep(300);
      assertThat(blocker.isDone()).isFalse();

      releaseLockLatch.countDown();
      lockHolder.get(5, TimeUnit.SECONDS);
      blocker.get(5, TimeUnit.SECONDS);

      // then: 락 해제 후에야 차단이 반영된다
      assertThat(chatUserBlockRepository.existsBlockBetween(user.getId(), partner.getId())).isTrue();
    } finally {
      executorService.shutdownNow();
    }
  }

  private void awaitQuietly(CountDownLatch latch) {
    try {
      assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
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
