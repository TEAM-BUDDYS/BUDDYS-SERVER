package org.sopt.buddys.domain.place.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.domain.course.repository.CourseImageRepository;
import org.sopt.buddys.domain.image.entity.ImageDomain;
import org.sopt.buddys.domain.image.service.UploadedImageUrlMatcher;
import org.sopt.buddys.domain.place.client.GooglePlacesClient;
import org.sopt.buddys.domain.place.client.UnsplashClient;
import org.sopt.buddys.domain.place.client.dto.GooglePhoto;
import org.sopt.buddys.domain.place.client.dto.unsplash.UnsplashImage;
import org.sopt.buddys.domain.place.code.PlaceErrorCode;
import org.sopt.buddys.domain.place.config.PlacePhotoProperties;
import org.sopt.buddys.domain.place.config.UnsplashProperties;
import org.sopt.buddys.domain.place.repository.PlacePhotoCacheRepository;
import org.sopt.buddys.domain.place.repository.PlacePhotoCacheRepository.CachedPhoto;
import org.sopt.buddys.domain.place.repository.PlaceRepository;
import org.sopt.buddys.domain.place.service.result.PlacePhoto;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

/**
 * 장소 대표 사진을 찾는다. 구글 사진을 우선 쓰고, 구글을 쓸 수 없으면 대체 사진으로 넘어간다.
 *
 * <ol>
 *   <li>구글 Places 사진: 캐시 우선, 캐시가 없으면 일일 한도 안에서만 구글을 호출한다</li>
 *   <li>사용자가 코스에 올린 사진 (장소가 하나뿐인 일차의 사진): 구글 한도 초과·장애·사진 없음일 때</li>
 *   <li>Unsplash 사진: 장소가 속한 도시로 검색한 사진</li>
 * </ol>
 * 모두 없으면 PLACE_PHOTO_NOT_FOUND(404)를 던지고, 앱이 기본 이미지를 보여준다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class PlacePhotoService {

  // 구글 API 일일 할당량은 미국 태평양 시간 자정에 초기화된다. 서버 카운터도 같은 기준으로 센다.
  private static final ZoneId GOOGLE_QUOTA_ZONE = ZoneId.of("America/Los_Angeles");
  private static final String METRIC_RESOLVED = "place.photo.resolved";
  private static final String METRIC_GOOGLE_QUOTA_EXCEEDED = "place.photo.google.quota.exceeded";
  private static final String METRIC_GOOGLE_FETCH_IN_PROGRESS = "place.photo.google.fetch.in-progress";
  // 구글 호출 2번(GetPlace + GetPhotoMedia)의 최대 소요 시간(연결 3초 + 응답 5초씩)보다 길게 잡는다.
  private static final Duration GOOGLE_FETCH_LOCK_TTL = Duration.ofSeconds(20);
  private static final String SOURCE_TAG = "source";
  private static final String SOURCE_NONE = "NONE";
  // 업로드 경로가 아닌 예전 URL이 섞여 있을 수 있어 후보를 몇 개 더 가져와 거른다.
  private static final int USER_PHOTO_CANDIDATES = 5;

  private final CourseImageRepository courseImageRepository;
  private final PlaceRepository placeRepository;
  private final PlacePhotoCacheRepository placePhotoCacheRepository;
  private final GooglePlacesClient googlePlacesClient;
  private final UnsplashClient unsplashClient;
  private final UploadedImageUrlMatcher uploadedImageUrlMatcher;
  private final PlacePhotoProperties placePhotoProperties;
  private final UnsplashProperties unsplashProperties;
  private final MeterRegistry meterRegistry;

  public PlacePhoto getPhoto(String googlePlaceId, int maxWidth) {
    // 리다이렉트 Location으로 쓸 수 없는 URL은 건너뛰고 다음 소스로 넘어간다(컨트롤러의 URI 변환 실패로 500이 나지 않게).
    Optional<PlacePhoto> photo = findGooglePhoto(googlePlaceId, maxWidth)
        .filter(p -> isRedirectable(p.url()))
        .or(() -> findUserPhoto(googlePlaceId))
        .or(() -> findUnsplashPhoto(googlePlaceId, maxWidth).filter(p -> isRedirectable(p.url())));

    meterRegistry.counter(METRIC_RESOLVED, SOURCE_TAG, photo.map(p -> p.source().name()).orElse(SOURCE_NONE))
        .increment();
    return photo.orElseThrow(() -> new BaseException(PlaceErrorCode.PLACE_PHOTO_NOT_FOUND));
  }

  private Optional<PlacePhoto> findUserPhoto(String googlePlaceId) {
    // 리다이렉트 대상이 되므로 우리 업로드 경로의 URL만 쓴다(오픈 리다이렉트 방지).
    return courseImageRepository.findImageUrlsOfSinglePlaceDays(googlePlaceId, PageRequest.of(0, USER_PHOTO_CANDIDATES))
        .stream()
        .filter(url -> uploadedImageUrlMatcher.matches(url, ImageDomain.COURSE))
        .filter(this::isRedirectable)
        .findFirst()
        .map(PlacePhoto::user);
  }

  private Optional<PlacePhoto> findGooglePhoto(String googlePlaceId, int maxWidth) {
    Optional<CachedPhoto> cached = placePhotoCacheRepository.findGooglePhoto(googlePlaceId, maxWidth);
    if (cached.isPresent()) {
      return cached.filter(CachedPhoto::exists).map(c -> PlacePhoto.google(c.url()));
    }

    // 같은 장소를 동시에 요청하면 한 요청만 구글을 호출한다. 나머지는 한도를 쓰지 않고 대체 사진으로 넘어간다.
    Optional<String> lockToken = placePhotoCacheRepository.tryLockGoogleFetch(
        googlePlaceId, maxWidth, GOOGLE_FETCH_LOCK_TTL);
    if (lockToken.isEmpty()) {
      meterRegistry.counter(METRIC_GOOGLE_FETCH_IN_PROGRESS).increment();
      return Optional.empty();
    }
    try {
      // 캐시를 확인한 뒤 락을 잡기 전에, 앞선 요청이 조회를 끝내고 캐시를 채웠을 수 있다.
      Optional<CachedPhoto> cachedAfterLock = placePhotoCacheRepository.findGooglePhoto(googlePlaceId, maxWidth);
      if (cachedAfterLock.isPresent()) {
        return cachedAfterLock.filter(CachedPhoto::exists).map(c -> PlacePhoto.google(c.url()));
      }
      return fetchGooglePhoto(googlePlaceId, maxWidth);
    } finally {
      placePhotoCacheRepository.releaseGoogleFetchLock(googlePlaceId, maxWidth, lockToken.get());
    }
  }

  private Optional<PlacePhoto> fetchGooglePhoto(String googlePlaceId, int maxWidth) {
    LocalDate today = LocalDate.now(GOOGLE_QUOTA_ZONE);
    if (!placePhotoCacheRepository.tryAcquireGoogleQuota(today, placePhotoProperties.googleDailyLimit())) {
      meterRegistry.counter(METRIC_GOOGLE_QUOTA_EXCEEDED).increment();
      return Optional.empty();
    }

    try {
      List<GooglePhoto> photos = googlePlacesClient.getPlaceDetails(googlePlaceId).photosOrEmpty();
      if (photos.isEmpty()) {
        placePhotoCacheRepository.saveGooglePhoto(
            googlePlaceId, maxWidth, CachedPhoto.notFound(), placePhotoProperties.notFoundCacheTtl());
        return Optional.empty();
      }
      String photoUri = googlePlacesClient.getPhotoMediaUri(photos.get(0).name(), maxWidth);
      placePhotoCacheRepository.saveGooglePhoto(
          googlePlaceId, maxWidth, CachedPhoto.of(photoUri), placePhotoProperties.googleUriCacheTtl());
      return Optional.of(PlacePhoto.google(photoUri));
    } catch (BaseException e) {
      // 구글 장애·할당량 초과(429)는 대체 사진으로 넘어간다. 일시적 실패일 수 있으니 "사진 없음"으로 캐시하지 않는다.
      log.warn("[PlacePhoto] 구글 사진 조회 실패, 대체 사진으로 넘어갑니다. placeId={}", googlePlaceId);
      return Optional.empty();
    }
  }

  private Optional<PlacePhoto> findUnsplashPhoto(String googlePlaceId, int maxWidth) {
    if (!unsplashProperties.enabled()) {
      return Optional.empty();
    }
    Optional<String> keyword = placeRepository.findPhotoSearchKeyword(googlePlaceId)
        .map(String::trim)
        .filter(k -> !k.isEmpty());
    if (keyword.isEmpty()) {
      return Optional.empty();
    }

    String cacheKey = keyword.get().toLowerCase(Locale.ROOT);
    Optional<CachedPhoto> cached = placePhotoCacheRepository.findFallbackPhoto(cacheKey);
    if (cached.isPresent()) {
      return cached.filter(CachedPhoto::exists).map(c -> toUnsplashPhoto(c, maxWidth));
    }

    try {
      Optional<UnsplashImage> image = unsplashClient.searchFirstLandscape(keyword.get());
      if (image.isEmpty()) {
        placePhotoCacheRepository.saveFallbackPhoto(
            cacheKey, CachedPhoto.notFound(), placePhotoProperties.notFoundCacheTtl());
        return Optional.empty();
      }
      CachedPhoto photo = CachedPhoto.of(image.get().rawUrl(), image.get().authorName(), image.get().authorPageUrl());
      placePhotoCacheRepository.saveFallbackPhoto(cacheKey, photo, placePhotoProperties.fallbackCacheTtl());
      return Optional.of(toUnsplashPhoto(photo, maxWidth));
    } catch (RestClientException e) {
      log.warn("[PlacePhoto] Unsplash 사진 조회 실패 → keyword={}", keyword.get(), e);
      return Optional.empty();
    }
  }

  private boolean isRedirectable(String url) {
    try {
      URI uri = URI.create(url);
      if (uri.isAbsolute() && "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null) {
        return true;
      }
    } catch (IllegalArgumentException | NullPointerException e) {
      // 아래에서 로그를 남기고 건너뛴다.
    }
    log.warn("[PlacePhoto] 리다이렉트할 수 없는 사진 URL이라 건너뜁니다. url={}", url);
    return false;
  }

  private PlacePhoto toUnsplashPhoto(CachedPhoto photo, int maxWidth) {
    // Unsplash raw URL은 imgix 파라미터로 크기를 조절한다.
    String separator = photo.url().contains("?") ? "&" : "?";
    String sizedUrl = photo.url() + separator + "w=" + maxWidth + "&fit=max&fm=jpg&q=80";
    return PlacePhoto.unsplash(sizedUrl, photo.authorName(), photo.authorPageUrl());
  }
}
