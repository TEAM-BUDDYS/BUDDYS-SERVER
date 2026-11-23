package org.sopt.buddys.domain.place.repository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 장소 사진 조회 결과 캐시와 구글 일일 호출 카운터.
 *
 * <p>캐시는 비용 절감용 보조 수단이라 Valkey 장애 시 조회는 "캐시 없음", 저장은 무시로 처리한다.
 * 반면 구글 호출 한도는 비용 폭주를 막는 장치라 카운터를 확인할 수 없으면 호출을 막는다(fail-closed).
 *
 * <p>구글 약관상 사진 원본이나 photo name은 저장하지 않고, 짧게 유효한 최종 photoUri만 TTL을 두고 캐시한다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PlacePhotoCacheRepository {

  private static final String GOOGLE_PHOTO_KEY_PREFIX = "place:photo:google:";
  private static final String FALLBACK_PHOTO_KEY_PREFIX = "place:photo:unsplash:";
  private static final String GOOGLE_DAILY_COUNT_KEY_PREFIX = "place:photo:google-count:";
  private static final Duration DAILY_COUNT_TTL = Duration.ofDays(2);
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
  // INCR와 첫 증가 시 EXPIRE를 한 번에 처리해 TTL 없는 카운터 키가 남지 않게 한다.
  private static final DefaultRedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>(
      "local count = redis.call('INCR', KEYS[1]); "
          + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end; "
          + "return count",
      Long.class
  );

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public Optional<CachedPhoto> findGooglePhoto(String googlePlaceId, int maxWidth) {
    return find(googlePhotoKey(googlePlaceId, maxWidth));
  }

  public void saveGooglePhoto(String googlePlaceId, int maxWidth, CachedPhoto photo, Duration ttl) {
    save(googlePhotoKey(googlePlaceId, maxWidth), photo, ttl);
  }

  public Optional<CachedPhoto> findFallbackPhoto(String keyword) {
    return find(FALLBACK_PHOTO_KEY_PREFIX + keyword);
  }

  public void saveFallbackPhoto(String keyword, CachedPhoto photo, Duration ttl) {
    save(FALLBACK_PHOTO_KEY_PREFIX + keyword, photo, ttl);
  }

  /**
   * 오늘 구글 호출 횟수를 1 늘리고, 한도 이내면 true를 반환한다.
   * 카운터를 확인할 수 없으면(Valkey 장애) 비용 보호를 위해 false를 반환한다.
   */
  public boolean tryAcquireGoogleQuota(LocalDate date, int dailyLimit) {
    try {
      Long count = redisTemplate.execute(
          INCREMENT_WITH_TTL,
          List.of(GOOGLE_DAILY_COUNT_KEY_PREFIX + date.format(DATE_FORMAT)),
          String.valueOf(DAILY_COUNT_TTL.toSeconds())
      );
      return count != null && count <= dailyLimit;
    } catch (DataAccessException e) {
      log.warn("[PlacePhotoCache] 구글 호출 카운터 확인 실패, 구글 호출을 막습니다.", e);
      return false;
    }
  }

  private Optional<CachedPhoto> find(String key) {
    try {
      String value = redisTemplate.opsForValue().get(key);
      if (value == null) {
        return Optional.empty();
      }
      return Optional.of(objectMapper.readValue(value, CachedPhoto.class));
    } catch (DataAccessException | JacksonException e) {
      log.warn("[PlacePhotoCache] 캐시 조회 실패 → key={}", key, e);
      return Optional.empty();
    }
  }

  private void save(String key, CachedPhoto photo, Duration ttl) {
    try {
      redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(photo), ttl);
    } catch (DataAccessException | JacksonException e) {
      log.warn("[PlacePhotoCache] 캐시 저장 실패 → key={}", key, e);
    }
  }

  private String googlePhotoKey(String googlePlaceId, int maxWidth) {
    return GOOGLE_PHOTO_KEY_PREFIX + googlePlaceId + ":" + maxWidth;
  }

  /**
   * 캐시된 사진. url이 null이면 "사진 없음"을 캐시한 것이다.
   */
  public record CachedPhoto(String url, String authorName, String authorPageUrl) {

    public static CachedPhoto of(String url) {
      return new CachedPhoto(url, null, null);
    }

    public static CachedPhoto of(String url, String authorName, String authorPageUrl) {
      return new CachedPhoto(url, authorName, authorPageUrl);
    }

    public static CachedPhoto notFound() {
      return new CachedPhoto(null, null, null);
    }

    public boolean exists() {
      return url != null;
    }
  }
}
