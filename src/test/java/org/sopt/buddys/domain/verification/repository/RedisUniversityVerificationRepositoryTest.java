package org.sopt.buddys.domain.verification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.lettuce.core.cluster.SlotHash;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.verification.entity.UniversityVerification;
import org.sopt.buddys.domain.verification.repository.UniversityVerificationRepository.Status;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RedisUniversityVerificationRepositoryTest {

  private static final String KEY_PREFIX = "verification:university:user:";
  private static final long USER_ID = 1L;
  private static final long UNIVERSITY_ID = 10L;
  private static final String EMAIL = "student@university.ac.kr";
  private static final Duration TTL = Duration.ofMinutes(15);
  private static final int MAX_ATTEMPTS = 5;

  @Container
  static GenericContainer<?> valkey = new GenericContainer<>(
      DockerImageName.parse("valkey/valkey:9-alpine")
  ).withExposedPorts(6379);

  private LettuceConnectionFactory connectionFactory;
  private StringRedisTemplate redisTemplate;
  private RedisUniversityVerificationRepository repository;

  @BeforeEach
  void setUp() {
    RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(
        valkey.getHost(),
        valkey.getMappedPort(6379)
    );
    connectionFactory = new LettuceConnectionFactory(configuration);
    connectionFactory.afterPropertiesSet();

    redisTemplate = new StringRedisTemplate(connectionFactory);
    redisTemplate.afterPropertiesSet();
    redisTemplate.delete(key(USER_ID));
    repository = new RedisUniversityVerificationRepository(redisTemplate);
  }

  @AfterEach
  void tearDown() {
    connectionFactory.destroy();
  }

  @DisplayName("인증 정보를 TTL로 저장하고 올바른 코드로만 조회된다")
  @Test
  void saveAndFindByUserIdAndCode() {
    // given
    UniversityVerification verification = UniversityVerification.issue(USER_ID, UNIVERSITY_ID, EMAIL);

    // when
    repository.save(verification, TTL);

    // then
    assertThat(repository.verifyCode(USER_ID, verification.code(), MAX_ATTEMPTS).verification())
        .isEqualTo(verification);
    assertThat(repository.verifyCode(USER_ID, "ZZZZZZ", MAX_ATTEMPTS).status())
        .isEqualTo(Status.INVALID);
    assertThat(redisTemplate.getExpire(key(USER_ID)))
        .isPositive()
        .isLessThanOrEqualTo(TTL.toSeconds());
    assertThat(redisTemplate.hasKey(attemptsKey(USER_ID))).isTrue();
    assertThat(SlotHash.getSlot(key(USER_ID)))
        .isEqualTo(SlotHash.getSlot(attemptsKey(USER_ID)));
  }

  @DisplayName("같은 사용자가 재발송하면 이전 코드는 무효화된다")
  @Test
  void saveAgain_invalidatesPreviousCode() {
    // given
    UniversityVerification previous = UniversityVerification.issue(USER_ID, UNIVERSITY_ID, EMAIL);
    UniversityVerification current = UniversityVerification.issue(USER_ID, UNIVERSITY_ID, EMAIL);
    repository.save(previous, TTL);

    // when
    repository.save(current, TTL);

    // then
    assertThat(repository.verifyCode(USER_ID, previous.code(), MAX_ATTEMPTS).status())
        .isEqualTo(Status.INVALID);
    assertThat(repository.verifyCode(USER_ID, current.code(), MAX_ATTEMPTS).verification())
        .isEqualTo(current);
  }

  @DisplayName("조건부 삭제는 재발송으로 갱신된 코드를 삭제하지 않는다")
  @Test
  void deleteIfMatches_doesNotDeleteRefreshedCode() {
    // given
    UniversityVerification previous = UniversityVerification.issue(USER_ID, UNIVERSITY_ID, EMAIL);
    UniversityVerification current = UniversityVerification.issue(USER_ID, UNIVERSITY_ID, EMAIL);
    repository.save(current, TTL);

    // when
    repository.deleteIfMatches(previous);

    // then
    assertThat(repository.verifyCode(USER_ID, current.code(), MAX_ATTEMPTS).verification())
        .isEqualTo(current);

    // when
    repository.deleteIfMatches(current);

    // then
    assertThat(repository.verifyCode(USER_ID, current.code(), MAX_ATTEMPTS).status())
        .isEqualTo(Status.INVALID);
  }

  @DisplayName("인증 코드가 제한 횟수만큼 틀리면 올바른 코드도 더 이상 사용할 수 없다")
  @Test
  void verifyCode_exceedingMaxAttempts_invalidatesCode() {
    // given
    UniversityVerification verification = UniversityVerification.issue(USER_ID, UNIVERSITY_ID, EMAIL);
    repository.save(verification, TTL);

    // when
    for (int attempt = 0; attempt < MAX_ATTEMPTS - 1; attempt++) {
      assertThat(repository.verifyCode(USER_ID, "WRONG" + attempt, MAX_ATTEMPTS).status())
          .isEqualTo(Status.INVALID);
    }
    assertThat(repository.verifyCode(USER_ID, "WRONG4", MAX_ATTEMPTS).status())
        .isEqualTo(Status.ATTEMPT_LIMIT_EXCEEDED);

    // then
    assertThat(repository.verifyCode(USER_ID, verification.code(), MAX_ATTEMPTS).status())
        .isEqualTo(Status.ATTEMPT_LIMIT_EXCEEDED);
    assertThat(redisTemplate.hasKey(key(USER_ID))).isFalse();
  }

  @DisplayName("새 인증 코드를 발급하면 이전 코드의 실패 횟수도 초기화된다")
  @Test
  void saveAgain_resetsFailedAttempts() {
    // given
    UniversityVerification previous = UniversityVerification.issue(USER_ID, UNIVERSITY_ID, EMAIL);
    repository.save(previous, TTL);
    for (int attempt = 0; attempt < MAX_ATTEMPTS - 1; attempt++) {
      repository.verifyCode(USER_ID, "WRONG" + attempt, MAX_ATTEMPTS);
    }

    UniversityVerification current = UniversityVerification.issue(USER_ID, UNIVERSITY_ID, EMAIL);

    // when
    repository.save(current, TTL);

    // then
    for (int attempt = 0; attempt < MAX_ATTEMPTS - 1; attempt++) {
      assertThat(repository.verifyCode(USER_ID, "RETRY" + attempt, MAX_ATTEMPTS).status())
          .isEqualTo(Status.INVALID);
    }
    assertThat(repository.verifyCode(USER_ID, current.code(), MAX_ATTEMPTS).verification())
        .isEqualTo(current);
  }

  private String key(long userId) {
    return KEY_PREFIX + "{" + userId + "}";
  }

  private String attemptsKey(long userId) {
    return key(userId) + ":attempts";
  }
}
