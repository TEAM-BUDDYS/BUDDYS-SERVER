package org.sopt.buddys.domain.place.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 장소 사진 조회 정책.
 *
 * @param googleDailyLimit     하루에 구글로 사진을 새로 조회할 수 있는 최대 횟수. 1회 조회 = GetPlace + GetPhotoMedia 호출.
 *                             이 횟수를 넘으면 구글을 부르지 않고 대체 사진(Unsplash)으로 넘어간다.
 * @param googleUriCacheTtl    구글 photoUri 캐시 유지 시간
 * @param fallbackCacheTtl     Unsplash 검색 결과 캐시 유지 시간
 * @param notFoundCacheTtl     "사진 없음" 결과 캐시 유지 시간 (같은 장소로 외부 API를 반복 호출하지 않기 위함)
 * @param fallbackResultCacheTtl 구글을 쓰지 못할 때 장소별 대체 사진 결과(사용자 사진/Unsplash/없음) 캐시 유지 시간.
 *                             구글 한도 초과 후 요청마다 DB 조회가 반복되지 않게 한다.
 * @param clientCacheMaxAge    사진 리다이렉트 응답의 Cache-Control max-age
 */
@Validated
@ConfigurationProperties(prefix = "place.photo")
public record PlacePhotoProperties(
    @Positive int googleDailyLimit,
    @NotNull Duration googleUriCacheTtl,
    @NotNull Duration fallbackCacheTtl,
    @NotNull Duration notFoundCacheTtl,
    @NotNull Duration fallbackResultCacheTtl,
    @NotNull Duration clientCacheMaxAge
) {

  public PlacePhotoProperties {
    requirePositive(googleUriCacheTtl, "googleUriCacheTtl");
    requirePositive(fallbackCacheTtl, "fallbackCacheTtl");
    requirePositive(notFoundCacheTtl, "notFoundCacheTtl");
    requirePositive(fallbackResultCacheTtl, "fallbackResultCacheTtl");
    requirePositive(clientCacheMaxAge, "clientCacheMaxAge");
  }

  private static void requirePositive(Duration duration, String name) {
    if (duration != null && (duration.isZero() || duration.isNegative())) {
      throw new IllegalArgumentException(name + " must be positive");
    }
  }
}
