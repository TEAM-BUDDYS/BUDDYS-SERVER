package org.sopt.buddys.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.sopt.buddys.domain.user.entity.AccountStatus;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.event.UserWithdrawnEvent;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Import(UserWithdrawalConcurrencyTest.WithdrawalEventCounter.class)
class UserWithdrawalConcurrencyTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private WithdrawalEventCounter eventCounter;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @BeforeEach
  void setUp() {
    userRepository.deleteAllInBatch();
    eventCounter.reset();
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteAllInBatch();
  }

  @DisplayName("동시에 회원 탈퇴를 요청해도 익명화와 후속 처리는 한 번만 수행한다")
  @Test
  void withdraw_concurrentRequests_processesWithdrawalOnce() throws Exception {
    // given
    User user = userRepository.saveAndFlush(User.builder()
        .email("test@test.com")
        .provider(AuthProvider.KAKAO)
        .providerId("provider-id")
        .nickname("버디")
        .build());
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    CountDownLatch readyLatch = new CountDownLatch(2);
    CountDownLatch startLatch = new CountDownLatch(1);

    try {
      Future<?> firstRequest = executorService.submit(
          () -> withdrawAtSameTime(user.getId(), readyLatch, startLatch)
      );
      Future<?> secondRequest = executorService.submit(
          () -> withdrawAtSameTime(user.getId(), readyLatch, startLatch)
      );
      assertThat(readyLatch.await(3, TimeUnit.SECONDS)).isTrue();

      // when
      startLatch.countDown();
      firstRequest.get(5, TimeUnit.SECONDS);
      secondRequest.get(5, TimeUnit.SECONDS);

      // then
      User withdrawnUser = userRepository.findById(user.getId()).orElseThrow();
      assertThat(withdrawnUser.getAccountStatus()).isEqualTo(AccountStatus.WITHDRAWN);
      assertThat(withdrawnUser.getDeletedAt()).isNotNull();
      assertThat(eventCounter.count()).isEqualTo(1);
    } finally {
      executorService.shutdownNow();
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @DisplayName("알림 변경과 탈퇴는 순서와 관계없이 익명화된 탈퇴 상태를 유지한다")
  void notificationAndWithdrawal_preserveAnonymization(boolean notificationFirst) throws Exception {
    User user = userRepository.saveAndFlush(User.builder()
        .email("private@example.com").provider(AuthProvider.KAKAO)
        .providerId("private-provider").nickname("원래닉네임").build());
    ExecutorService executor = Executors.newSingleThreadExecutor();
    AtomicReference<Future<?>> concurrent = new AtomicReference<>();
    CountDownLatch started = new CountDownLatch(1);
    try {
      new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
        if (notificationFirst) {
          userService.updateNotificationSetting(user.getId(), true);
        } else {
          userService.withdraw(user.getId());
        }
        concurrent.set(executor.submit(() -> {
          started.countDown();
          if (notificationFirst) {
            userService.withdraw(user.getId());
          } else {
            assertThatThrownBy(() -> userService.updateNotificationSetting(user.getId(), true))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
          }
        }));
        try {
          assertThat(started.await(3, TimeUnit.SECONDS)).isTrue();
          assertThatThrownBy(() -> concurrent.get().get(200, TimeUnit.MILLISECONDS))
              .isInstanceOf(TimeoutException.class);
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException(exception);
        }
      });
      concurrent.get().get(5, TimeUnit.SECONDS);
      User result = userRepository.findById(user.getId()).orElseThrow();
      assertThat(result.getDeletedAt()).isNotNull();
      assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.WITHDRAWN);
      assertThat(result.isNotificationEnabled()).isFalse();
      assertThat(result.getEmail()).endsWith("@deleted.invalid");
      assertThat(result.getProviderId()).startsWith("withdrawn:");
      assertThat(result.getNickname()).startsWith("탈퇴한 사용자_");
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  private void withdrawAtSameTime(
      Long userId,
      CountDownLatch readyLatch,
      CountDownLatch startLatch
  ) {
    readyLatch.countDown();
    try {
      assertThat(startLatch.await(3, TimeUnit.SECONDS)).isTrue();
      userService.withdraw(userId);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  static class WithdrawalEventCounter {

    private final AtomicInteger count = new AtomicInteger();

    @EventListener
    public void handle(UserWithdrawnEvent event) {
      count.incrementAndGet();
    }

    int count() {
      return count.get();
    }

    void reset() {
      count.set(0);
    }
  }
}
