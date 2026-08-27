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
    repository = new RedisUniversityVerificationRepository(redisTemplate);
  }

  @AfterEach
  void tearDown() {
    connectionFactory.destroy();
  }

  @DisplayName("인증 정보를 TTL로 저장하고 올바른 토큰으로 조회한다")
  @Test
  void saveAndFindByToken() {
    // given
    UniversityVerification verification = UniversityVerification.issue(
        1L,
        10L,
        "student@university.ac.kr"
    );

    // when
    repository.save(verification, Duration.ofMinutes(15));

    // then
    assertThat(repository.findByToken(verification.token())).contains(verification);
    assertThat(redisTemplate.getExpire(KEY_PREFIX + verification.userId()))
        .isPositive()
        .isLessThanOrEqualTo(Duration.ofMinutes(15).toSeconds());
  }

  @DisplayName("같은 사용자가 재발송하면 이전 토큰은 즉시 무효화된다")
  @Test
  void saveAgain_invalidatesPreviousToken() {
    // given
    UniversityVerification previous = UniversityVerification.issue(
        1L,
        10L,
        "student@university.ac.kr"
    );
    UniversityVerification current = UniversityVerification.issue(
        1L,
        10L,
        "student@university.ac.kr"
    );
    repository.save(previous, Duration.ofMinutes(15));

    // when
    repository.save(current, Duration.ofMinutes(15));

    // then
    assertThat(repository.findByToken(previous.token())).isEmpty();
    assertThat(repository.findByToken(current.token())).contains(current);
  }

  @DisplayName("이전 요청의 조건부 삭제가 새로 발급한 토큰을 삭제하지 않는다")
  @Test
  void deleteIfTokenMatches_doesNotDeleteNewToken() {
    // given
    UniversityVerification previous = UniversityVerification.issue(
        1L,
        10L,
        "student@university.ac.kr"
    );
    UniversityVerification current = UniversityVerification.issue(
        1L,
        10L,
        "student@university.ac.kr"
    );
    repository.save(current, Duration.ofMinutes(15));

    // when
    repository.deleteIfTokenMatches(previous);

    // then
    assertThat(repository.findByToken(current.token())).contains(current);

    // when
    repository.deleteIfTokenMatches(current);

    // then
    assertThat(repository.findByToken(current.token())).isEmpty();
  }
}
