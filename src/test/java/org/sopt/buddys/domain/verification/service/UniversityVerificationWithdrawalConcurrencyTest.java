package org.sopt.buddys.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.service.UserService;
import org.sopt.buddys.domain.verification.repository.RedisUniversityVerificationRepository;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class UniversityVerificationWithdrawalConcurrencyTest {

  private static final String EMAIL = "student@verification-test.ac.kr";

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Container
  static GenericContainer<?> redis = new GenericContainer<>("valkey/valkey:9-alpine")
      .withExposedPorts(6379);

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("buddys.redis.host", redis::getHost);
    registry.add("buddys.redis.port", () -> redis.getMappedPort(6379));
  }

  @Autowired private UniversityVerificationService verificationService;
  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private StringRedisTemplate redisTemplate;
  @Autowired private PlatformTransactionManager transactionManager;
  @MockitoBean private UniversityVerificationMailSender mailSender;
  @MockitoSpyBean private RedisUniversityVerificationRepository verificationRepository;
  private Long userId;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("INSERT INTO country (id, name, iso_code) VALUES (990001, '인증테스트', 'ZZ')");
    jdbcTemplate.update("""
        INSERT INTO university (id, country_id, name, domain)
        VALUES (990001, 990001, '인증테스트학교', 'verification-test.ac.kr')
        """);
    userId = userRepository.saveAndFlush(User.builder()
        .email(EMAIL).provider(AuthProvider.KAKAO).providerId("verification-test")
        .nickname("인증테스트").build()).getId();
  }

  @AfterEach
  void tearDown() {
    redisTemplate.delete(keys());
    userRepository.deleteAllInBatch();
    jdbcTemplate.update("DELETE FROM university WHERE id = 990001");
    jdbcTemplate.update("DELETE FROM country WHERE id = 990001");
  }

  @DisplayName("인증 발급이 먼저 시작되면 Redis 저장 후 탈퇴가 진행되어 인증 정보가 남지 않는다")
  @Test
  void issuanceFirst_withdrawalWaitsForRedisSaveAndDeletesIt() throws Exception {
    var executor = Executors.newFixedThreadPool(2);
    CountDownLatch saving = new CountDownLatch(1);
    CountDownLatch allowSave = new CountDownLatch(1);
    CountDownLatch withdrawing = new CountDownLatch(1);
    doAnswer(invocation -> {
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
      saving.countDown();
      await(allowSave);
      return invocation.callRealMethod();
    }).when(verificationRepository).save(any(), any());
    try {
      var issuance = executor.submit(() -> verificationService.sendVerification(userId, EMAIL));
      await(saving);
      var withdrawal = executor.submit(() -> {
        withdrawing.countDown();
        userService.withdraw(userId);
      });
      await(withdrawing);
      assertThatThrownBy(() -> withdrawal.get(200, TimeUnit.MILLISECONDS))
          .isInstanceOf(TimeoutException.class);
      allowSave.countDown();
      issuance.get(10, TimeUnit.SECONDS);
      withdrawal.get(10, TimeUnit.SECONDS);
      assertWithdrawnWithoutVerification();
    } finally {
      allowSave.countDown();
      executor.shutdownNow();
      assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }
  }

  @DisplayName("탈퇴가 먼저 시작되면 대기하던 인증 발급은 저장과 메일 전송 없이 거부된다")
  @Test
  void withdrawalFirst_issuanceWaitsAndRejectsWithdrawnUser() throws Exception {
    var executor = Executors.newSingleThreadExecutor();
    CountDownLatch issuing = new CountDownLatch(1);
    try {
      var issuance = new TransactionTemplate(transactionManager).execute(status -> {
        userService.withdraw(userId);
        var pending = executor.submit(() -> {
          issuing.countDown();
          assertThatThrownBy(() -> verificationService.sendVerification(userId, EMAIL))
              .isInstanceOf(BaseException.class)
              .extracting(exception -> ((BaseException) exception).getErrorCode())
              .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        });
        await(issuing);
        assertThatThrownBy(() -> pending.get(200, TimeUnit.MILLISECONDS))
            .isInstanceOf(TimeoutException.class);
        return pending;
      });
      issuance.get(10, TimeUnit.SECONDS);
      verifyNoInteractions(mailSender);
      assertWithdrawnWithoutVerification();
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }
  }

  @DisplayName("메일 전송은 발급 트랜잭션이 종료된 뒤 실행되어 탈퇴를 기다리게 하지 않는다")
  @Test
  void mailIsSentOutsideIssueTransaction() {
    doAnswer(invocation -> {
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
      userService.withdraw(userId);
      return null;
    }).when(mailSender).send(any(), any(), any());

    verificationService.sendVerification(userId, EMAIL);

    assertWithdrawnWithoutVerification();
  }

  private List<String> keys() {
    String key = "verification:university:user:{" + userId + "}";
    return List.of(key, key + ":attempts");
  }

  private void assertWithdrawnWithoutVerification() {
    assertThat(userRepository.findById(userId).orElseThrow().getDeletedAt()).isNotNull();
    assertThat(redisTemplate.countExistingKeys(keys())).isZero();
  }

  private static void await(CountDownLatch latch) {
    try {
      assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(exception);
    }
  }
}
