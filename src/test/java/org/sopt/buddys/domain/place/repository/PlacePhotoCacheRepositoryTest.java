package org.sopt.buddys.domain.place.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.place.repository.PlacePhotoCacheRepository.CachedPhoto;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers
class PlacePhotoCacheRepositoryTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);

  @Container
  static GenericContainer<?> valkey = new GenericContainer<>(
      DockerImageName.parse("valkey/valkey:9-alpine")
  ).withExposedPorts(6379);

  private LettuceConnectionFactory connectionFactory;
  private StringRedisTemplate redisTemplate;
  private PlacePhotoCacheRepository repository;

  @BeforeEach
  void setUp() {
    connectionFactory = new LettuceConnectionFactory(
        new RedisStandaloneConfiguration(valkey.getHost(), valkey.getMappedPort(6379)));
    connectionFactory.afterPropertiesSet();
    redisTemplate = new StringRedisTemplate(connectionFactory);
    redisTemplate.afterPropertiesSet();
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    repository = new PlacePhotoCacheRepository(redisTemplate, JsonMapper.builder().build());
  }

  @AfterEach
  void tearDown() {
    connectionFactory.destroy();
  }

  @DisplayName("구글 사진 URI를 장소·너비별로 TTL과 함께 저장하고 조회한다")
  @Test
  void googlePhoto_saveAndFindByWidth() {
    repository.saveGooglePhoto("place-1", 400, CachedPhoto.of("https://google/photo"), Duration.ofHours(1));

    assertThat(repository.findGooglePhoto("place-1", 400))
        .hasValueSatisfying(photo -> assertThat(photo.url()).isEqualTo("https://google/photo"));
    assertThat(repository.findGooglePhoto("place-1", 800)).isEmpty();
    assertThat(redisTemplate.getExpire("place:photo:google:place-1:400"))
        .isPositive()
        .isLessThanOrEqualTo(Duration.ofHours(1).toSeconds());
  }

  @DisplayName("'사진 없음'도 캐시해서 구분할 수 있다")
  @Test
  void googlePhoto_notFoundIsCached() {
    repository.saveGooglePhoto("place-1", 400, CachedPhoto.notFound(), Duration.ofHours(1));

    assertThat(repository.findGooglePhoto("place-1", 400))
        .hasValueSatisfying(photo -> assertThat(photo.exists()).isFalse());
  }

  @DisplayName("대체 사진은 작가 정보까지 저장한다")
  @Test
  void fallbackPhoto_savesAttribution() {
    repository.saveFallbackPhoto("paris",
        CachedPhoto.of("https://images.unsplash.com/raw?ixid=1", "홍길동", "https://unsplash.com/@hong"),
        Duration.ofDays(7));

    assertThat(repository.findFallbackPhoto("paris"))
        .hasValue(CachedPhoto.of("https://images.unsplash.com/raw?ixid=1", "홍길동", "https://unsplash.com/@hong"));
  }

  @DisplayName("구글 일일 호출 수가 한도 이내일 때만 허용하고 카운터에 TTL이 걸린다")
  @Test
  void tryAcquireGoogleQuota_allowsUpToLimit() {
    assertThat(repository.tryAcquireGoogleQuota(TODAY, 2)).isTrue();
    assertThat(repository.tryAcquireGoogleQuota(TODAY, 2)).isTrue();
    assertThat(repository.tryAcquireGoogleQuota(TODAY, 2)).isFalse();
    assertThat(repository.tryAcquireGoogleQuota(TODAY.plusDays(1), 2)).isTrue();

    assertThat(redisTemplate.getExpire("place:photo:google-count:20261118"))
        .isPositive()
        .isLessThanOrEqualTo(Duration.ofDays(2).toSeconds());
  }

  @DisplayName("Valkey에 연결할 수 없으면 캐시는 없는 것으로, 구글 호출은 막는 것으로 처리한다")
  @Test
  void valkeyUnavailable_cacheMissAndQuotaDenied() {
    LettuceConnectionFactory brokenFactory = new LettuceConnectionFactory(
        new RedisStandaloneConfiguration("localhost", 1));
    brokenFactory.afterPropertiesSet();
    StringRedisTemplate brokenTemplate = new StringRedisTemplate(brokenFactory);
    brokenTemplate.afterPropertiesSet();
    PlacePhotoCacheRepository brokenRepository =
        new PlacePhotoCacheRepository(brokenTemplate, JsonMapper.builder().build());

    try {
      assertThat(brokenRepository.findGooglePhoto("place-1", 400)).isEmpty();
      brokenRepository.saveGooglePhoto("place-1", 400, CachedPhoto.of("uri"), Duration.ofHours(1));
      assertThat(brokenRepository.tryAcquireGoogleQuota(TODAY, 100)).isFalse();
      assertThat(brokenRepository.tryLockGoogleFetch("place-1", 400, Duration.ofSeconds(20))).isEmpty();
      brokenRepository.releaseGoogleFetchLock("place-1", 400, "token");
    } finally {
      brokenFactory.destroy();
    }
  }

  @DisplayName("구글 조회 락은 장소·너비별로 한 요청만 잡을 수 있고 TTL이 걸린다")
  @Test
  void tryLockGoogleFetch_onlyOneHolder() {
    assertThat(repository.tryLockGoogleFetch("place-1", 400, Duration.ofSeconds(20))).isPresent();
    assertThat(repository.tryLockGoogleFetch("place-1", 400, Duration.ofSeconds(20))).isEmpty();
    assertThat(repository.tryLockGoogleFetch("place-1", 800, Duration.ofSeconds(20))).isPresent();

    assertThat(redisTemplate.getExpire("place:photo:google-lock:place-1:400"))
        .isPositive()
        .isLessThanOrEqualTo(20);
  }

  @DisplayName("구글 조회 락은 잡은 요청의 토큰으로만 해제된다")
  @Test
  void releaseGoogleFetchLock_onlyByOwner() {
    String token = repository.tryLockGoogleFetch("place-1", 400, Duration.ofSeconds(20)).orElseThrow();

    repository.releaseGoogleFetchLock("place-1", 400, "other-token");
    assertThat(repository.tryLockGoogleFetch("place-1", 400, Duration.ofSeconds(20))).isEmpty();

    repository.releaseGoogleFetchLock("place-1", 400, token);
    assertThat(repository.tryLockGoogleFetch("place-1", 400, Duration.ofSeconds(20))).isPresent();
  }
}
