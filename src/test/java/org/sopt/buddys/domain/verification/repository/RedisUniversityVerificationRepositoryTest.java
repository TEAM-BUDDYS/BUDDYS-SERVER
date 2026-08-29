package org.sopt.buddys.domain.verification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.verification.entity.UniversityVerification;
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
    redisTemplate.delete(KEY_PREFIX + USER_ID);
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
    assertThat(repository.findByUserIdAndCode(USER_ID, verification.code())).contains(verification);
    assertThat(repository.findByUserIdAndCode(USER_ID, "ZZZZZZ")).isEmpty();
    assertThat(redisTemplate.getExpire(KEY_PREFIX + USER_ID))
        .isPositive()
        .isLessThanOrEqualTo(TTL.toSeconds());
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
    assertThat(repository.findByUserIdAndCode(USER_ID, previous.code())).isEmpty();
    assertThat(repository.findByUserIdAndCode(USER_ID, current.code())).contains(current);
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
    assertThat(repository.findByUserIdAndCode(USER_ID, current.code())).contains(current);

    // when
    repository.deleteIfMatches(current);

    // then
    assertThat(repository.findByUserIdAndCode(USER_ID, current.code())).isEmpty();
  }
}
