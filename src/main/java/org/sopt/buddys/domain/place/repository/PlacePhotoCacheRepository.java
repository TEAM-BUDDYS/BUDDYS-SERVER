package org.sopt.buddys.domain.place.repository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.domain.place.service.result.PlacePhoto;
import org.sopt.buddys.domain.place.service.result.PlacePhotoSource;
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
  private static final String UNSPLASH_SUSPENDED_KEY = "place:photo:unsplash-suspended";
  private static final String FALLBACK_RESULT_KEY_PREFIX = "place:photo:fallback-result:";
  private static final String GOOGLE_FETCH_LOCK_KEY_PREFIX = "place:photo:google-lock:";
  private static final Duration DAILY_COUNT_TTL = Duration.ofDays(2);
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
  // INCR와 첫 증가 시 EXPIRE를 한 번에 처리해 TTL 없는 카운터 키가 남지 않게 한다.
  private static final DefaultRedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>(
      "local count = redis.call('INCR', KEYS[1]); "
          + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end; "
          + "return count",
      Long.class
  );
  // 내가 잡은 락(토큰 일치)일 때만 지운다. TTL이 지나 다른 요청이 잡은 락을 지우지 않기 위함이다.
  private static final DefaultRedisScript<Long> RELEASE_IF_OWNER = new DefaultRedisScript<>(
      "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
      Long.class
  );

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public Optional<CachedPhoto> findGooglePhoto(String googlePlaceId, int maxWidth) {
    return find(googlePhotoKey(googlePlaceId, maxWidth), CachedPhoto.class);
  }

  public void saveGooglePhoto(String googlePlaceId, int maxWidth, CachedPhoto photo, Duration ttl) {
    save(googlePhotoKey(googlePlaceId, maxWidth), photo, ttl);
  }

  public Optional<CachedPhoto> findFallbackPhoto(String keyword) {
    return find(FALLBACK_PHOTO_KEY_PREFIX + keyword, CachedPhoto.class);
  }

  public void saveFallbackPhoto(String keyword, CachedPhoto photo, Duration ttl) {
    save(FALLBACK_PHOTO_KEY_PREFIX + keyword, photo, ttl);
  }

  public Optional<CachedFallbackResult> findFallbackResult(String googlePlaceId, int maxWidth) {
    return find(fallbackResultKey(googlePlaceId, maxWidth), CachedFallbackResult.class);
  }

  public void saveFallbackResult(String googlePlaceId, int maxWidth, CachedFallbackResult result, Duration ttl) {
    save(fallbackResultKey(googlePlaceId, maxWidth), result, ttl);
  }

  /**
   * Unsplash 호출이 일시 중단 상태인지 확인한다. Valkey 장애면 중단하지 않은 것으로 본다(Unsplash는 무료라 막을 이유가 없다).
   */
  public boolean isUnsplashSuspended() {
    try {
      return Boolean.TRUE.equals(redisTemplate.hasKey(UNSPLASH_SUSPENDED_KEY));
    } catch (DataAccessException e) {
      log.warn("[PlacePhotoCache] Unsplash 중단 여부 확인 실패", e);
      return false;
    }
  }

  /**
   * Unsplash 호출을 일정 시간 중단한다(서킷 브레이커). 한도 초과·장애 시 요청마다 재호출하지 않기 위함이다.
   */
  public void suspendUnsplash(Duration duration) {
    try {
      redisTemplate.opsForValue().set(UNSPLASH_SUSPENDED_KEY, "1", duration);
    } catch (DataAccessException e) {
      log.warn("[PlacePhotoCache] Unsplash 중단 설정 실패", e);
    }
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

  /**
   * 같은 장소·너비의 구글 조회를 한 요청만 하도록 락을 잡는다. 성공하면 해제에 쓸 토큰을 반환한다.
   * 이미 다른 요청이 조회 중이거나 Valkey 장애면 빈 값을 반환한다(구글 호출을 하지 않는다).
   */
  public Optional<String> tryLockGoogleFetch(String googlePlaceId, int maxWidth, Duration ttl) {
    String token = UUID.randomUUID().toString();
    try {
      Boolean acquired = redisTemplate.opsForValue()
          .setIfAbsent(googleFetchLockKey(googlePlaceId, maxWidth), token, ttl);
      return Boolean.TRUE.equals(acquired) ? Optional.of(token) : Optional.empty();
    } catch (DataAccessException e) {
      log.warn("[PlacePhotoCache] 구글 조회 락 획득 실패, 구글 호출을 막습니다.", e);
      return Optional.empty();
    }
  }

  public void releaseGoogleFetchLock(String googlePlaceId, int maxWidth, String token) {
    try {
      redisTemplate.execute(RELEASE_IF_OWNER, List.of(googleFetchLockKey(googlePlaceId, maxWidth)), token);
    } catch (DataAccessException e) {
      // 해제에 실패해도 TTL이 지나면 풀린다.
      log.warn("[PlacePhotoCache] 구글 조회 락 해제 실패", e);
    }
  }

  private <T> Optional<T> find(String key, Class<T> type) {
    try {
      String value = redisTemplate.opsForValue().get(key);
      if (value == null) {
        return Optional.empty();
      }
      return Optional.of(objectMapper.readValue(value, type));
    } catch (DataAccessException | JacksonException e) {
      log.warn("[PlacePhotoCache] 캐시 조회 실패 → key={}", key, e);
      return Optional.empty();
    }
  }

  private void save(String key, Object value, Duration ttl) {
    try {
      redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
    } catch (DataAccessException | JacksonException e) {
      log.warn("[PlacePhotoCache] 캐시 저장 실패 → key={}", key, e);
    }
  }

  private String fallbackResultKey(String googlePlaceId, int maxWidth) {
    return FALLBACK_RESULT_KEY_PREFIX + googlePlaceId + ":" + maxWidth;
  }

  private String googleFetchLockKey(String googlePlaceId, int maxWidth) {
    return GOOGLE_FETCH_LOCK_KEY_PREFIX + googlePlaceId + ":" + maxWidth;
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

  /**
   * 장소별 대체 사진 결과. source가 null이면 "대체 사진 없음"을 캐시한 것이다.
   */
  public record CachedFallbackResult(PlacePhotoSource source, String url, String authorName, String authorPageUrl) {

    public static CachedFallbackResult of(PlacePhoto photo) {
      return new CachedFallbackResult(photo.source(), photo.url(), photo.authorName(), photo.authorPageUrl());
    }

    public static CachedFallbackResult notFound() {
      return new CachedFallbackResult(null, null, null, null);
    }

    public Optional<PlacePhoto> toPlacePhoto() {
      if (source == null || url == null) {
        return Optional.empty();
      }
      return Optional.of(new PlacePhoto(url, source, authorName, authorPageUrl));
    }
  }
}
