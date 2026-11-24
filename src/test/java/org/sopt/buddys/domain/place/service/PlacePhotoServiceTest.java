package org.sopt.buddys.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.course.repository.CourseImageRepository;
import org.sopt.buddys.domain.image.service.UploadedImageUrlMatcher;
import org.sopt.buddys.domain.place.client.GooglePlacesClient;
import org.sopt.buddys.domain.place.client.UnsplashClient;
import org.sopt.buddys.domain.place.client.dto.GooglePhoto;
import org.sopt.buddys.domain.place.client.dto.GooglePlaceDetailsResponse;
import org.sopt.buddys.domain.place.client.dto.unsplash.UnsplashImage;
import org.sopt.buddys.domain.place.code.PlaceErrorCode;
import org.sopt.buddys.domain.place.config.PlacePhotoProperties;
import org.sopt.buddys.domain.place.config.UnsplashProperties;
import org.sopt.buddys.domain.place.repository.PlacePhotoCacheRepository;
import org.sopt.buddys.domain.place.repository.PlacePhotoCacheRepository.CachedPhoto;
import org.sopt.buddys.domain.place.repository.PlaceRepository;
import org.sopt.buddys.domain.place.service.result.PlacePhoto;
import org.sopt.buddys.domain.place.service.result.PlacePhotoSource;
import org.sopt.buddys.global.aws.s3.S3PresignedUrlManager;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.RestClientException;

class PlacePhotoServiceTest {

  private static final String PLACE_ID = "place-1";
  private static final int WIDTH = 400;
  private static final int DAILY_LIMIT = 100;
  private static final Duration GOOGLE_TTL = Duration.ofHours(6);
  private static final Duration FALLBACK_TTL = Duration.ofDays(7);
  private static final Duration NOT_FOUND_TTL = Duration.ofDays(1);
  private static final String UNSPLASH_RAW = "https://images.unsplash.com/photo-1?ixid=abc";
  private static final String LOCK_TOKEN = "lock-token";
  private static final String COURSE_UPLOAD_PREFIX = "https://buddys-test-bucket.s3.ap-northeast-2.amazonaws.com/courses/";
  private static final String USER_PHOTO_URL = COURSE_UPLOAD_PREFIX + "0b6f2c1e-7a55-4a52-9d0c-3f1f6f2f9f10.jpg";

  private CourseImageRepository courseImageRepository;
  private PlaceRepository placeRepository;
  private PlacePhotoCacheRepository cacheRepository;
  private GooglePlacesClient googlePlacesClient;
  private UnsplashClient unsplashClient;
  private UploadedImageUrlMatcher uploadedImageUrlMatcher;
  private SimpleMeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    courseImageRepository = mock(CourseImageRepository.class);
    placeRepository = mock(PlaceRepository.class);
    cacheRepository = mock(PlacePhotoCacheRepository.class);
    googlePlacesClient = mock(GooglePlacesClient.class);
    unsplashClient = mock(UnsplashClient.class);
    S3PresignedUrlManager s3PresignedUrlManager = mock(S3PresignedUrlManager.class);
    given(s3PresignedUrlManager.buildPublicUrl("courses/")).willReturn(COURSE_UPLOAD_PREFIX);
    uploadedImageUrlMatcher = new UploadedImageUrlMatcher(s3PresignedUrlManager);
    meterRegistry = new SimpleMeterRegistry();

    given(courseImageRepository.findImageUrlsOfSinglePlaceDays(eq(PLACE_ID), any(Pageable.class)))
        .willReturn(List.of());
    given(cacheRepository.findGooglePhoto(anyString(), anyInt())).willReturn(Optional.empty());
    given(cacheRepository.findFallbackPhoto(anyString())).willReturn(Optional.empty());
    given(cacheRepository.tryAcquireGoogleQuota(any(), eq(DAILY_LIMIT))).willReturn(true);
    given(cacheRepository.tryLockGoogleFetch(anyString(), anyInt(), any())).willReturn(Optional.of(LOCK_TOKEN));
  }

  private PlacePhotoService service(boolean unsplashEnabled) {
    return new PlacePhotoService(
        courseImageRepository,
        placeRepository,
        cacheRepository,
        googlePlacesClient,
        unsplashClient,
        uploadedImageUrlMatcher,
        new PlacePhotoProperties(DAILY_LIMIT, GOOGLE_TTL, FALLBACK_TTL, NOT_FOUND_TTL, Duration.ofMinutes(30)),
        new UnsplashProperties(unsplashEnabled ? "access-key" : "", "https://api.unsplash.com/search/photos", "buddys"),
        meterRegistry
    );
  }

  private PlacePhotoService service() {
    return service(true);
  }

  private void givenGooglePhoto(String photoUri) {
    given(googlePlacesClient.getPlaceDetails(PLACE_ID))
        .willReturn(new GooglePlaceDetailsResponse(List.of(new GooglePhoto("places/place-1/photos/p1"))));
    given(googlePlacesClient.getPhotoMediaUri("places/place-1/photos/p1", WIDTH)).willReturn(photoUri);
  }

  private void givenUnsplashKeyword(String keyword) {
    given(placeRepository.findPhotoSearchKeyword(PLACE_ID)).willReturn(Optional.of(keyword));
  }

  @DisplayName("구글 사진을 쓸 수 있으면 사용자 사진이 있어도 구글 사진을 쓴다")
  @Test
  void getPhoto_googleAvailable_prefersGoogleOverUserPhoto() {
    given(courseImageRepository.findImageUrlsOfSinglePlaceDays(eq(PLACE_ID), any(Pageable.class)))
        .willReturn(List.of(USER_PHOTO_URL));
    givenGooglePhoto("https://google/new");

    PlacePhoto photo = service().getPhoto(PLACE_ID, WIDTH);

    assertThat(photo).isEqualTo(PlacePhoto.google("https://google/new"));
    then(courseImageRepository).shouldHaveNoInteractions();
  }

  @DisplayName("구글 일일 한도를 넘으면 구글을 부르지 않고 사용자 사진을 쓴다")
  @Test
  void getPhoto_quotaExceeded_returnsUserPhoto() {
    given(cacheRepository.tryAcquireGoogleQuota(any(), eq(DAILY_LIMIT))).willReturn(false);
    given(courseImageRepository.findImageUrlsOfSinglePlaceDays(eq(PLACE_ID), any(Pageable.class)))
        .willReturn(List.of(USER_PHOTO_URL));

    PlacePhoto photo = service().getPhoto(PLACE_ID, WIDTH);

    assertThat(photo).isEqualTo(PlacePhoto.user(USER_PHOTO_URL));
    then(googlePlacesClient).shouldHaveNoInteractions();
    then(unsplashClient).shouldHaveNoInteractions();
  }

  @DisplayName("구글 호출이 실패하면 사용자 사진을 쓴다")
  @Test
  void getPhoto_googleFails_returnsUserPhoto() {
    given(googlePlacesClient.getPlaceDetails(PLACE_ID))
        .willThrow(new BaseException(PlaceErrorCode.GOOGLE_PLACES_UNAVAILABLE));
    given(courseImageRepository.findImageUrlsOfSinglePlaceDays(eq(PLACE_ID), any(Pageable.class)))
        .willReturn(List.of(USER_PHOTO_URL));

    PlacePhoto photo = service().getPhoto(PLACE_ID, WIDTH);

    assertThat(photo).isEqualTo(PlacePhoto.user(USER_PHOTO_URL));
    then(unsplashClient).shouldHaveNoInteractions();
  }

  @DisplayName("구글 캐시가 있으면 한도를 쓰지 않고 캐시된 URI를 쓴다")
  @Test
  void getPhoto_googleCacheHit_returnsCachedUri() {
    given(cacheRepository.findGooglePhoto(PLACE_ID, WIDTH))
        .willReturn(Optional.of(CachedPhoto.of("https://google/cached")));

    PlacePhoto photo = service().getPhoto(PLACE_ID, WIDTH);

    assertThat(photo).isEqualTo(PlacePhoto.google("https://google/cached"));
    then(googlePlacesClient).shouldHaveNoInteractions();
    then(cacheRepository).should(never()).tryAcquireGoogleQuota(any(), anyInt());
  }

  @DisplayName("구글 캐시가 없고 한도 이내면 구글을 호출하고 결과를 캐시한다")
  @Test
  void getPhoto_withinQuota_callsGoogleAndCaches() {
    givenGooglePhoto("https://google/new");

    PlacePhoto photo = service().getPhoto(PLACE_ID, WIDTH);

    assertThat(photo).isEqualTo(PlacePhoto.google("https://google/new"));
    then(cacheRepository).should()
        .saveGooglePhoto(PLACE_ID, WIDTH, CachedPhoto.of("https://google/new"), GOOGLE_TTL);
    assertThat(meterRegistry.counter("place.photo.resolved", "source", "GOOGLE").count()).isEqualTo(1);
  }

  @DisplayName("구글 일일 한도를 넘으면 구글을 부르지 않고 도시 Unsplash 사진을 쓴다")
  @Test
  void getPhoto_quotaExceeded_fallsBackToUnsplash() {
    given(cacheRepository.tryAcquireGoogleQuota(any(), eq(DAILY_LIMIT))).willReturn(false);
    givenUnsplashKeyword("Paris");
    given(unsplashClient.searchFirstLandscape("Paris"))
        .willReturn(Optional.of(new UnsplashImage(UNSPLASH_RAW, "홍길동", "https://unsplash.com/@hong?utm_source=buddys")));

    PlacePhoto photo = service().getPhoto(PLACE_ID, WIDTH);

    assertThat(photo.source()).isEqualTo(PlacePhotoSource.UNSPLASH);
    assertThat(photo.url()).isEqualTo(UNSPLASH_RAW + "&w=400&fit=max&fm=jpg&q=80");
    assertThat(photo.authorName()).isEqualTo("홍길동");
    assertThat(photo.authorPageUrl()).isEqualTo("https://unsplash.com/@hong?utm_source=buddys");
    then(googlePlacesClient).shouldHaveNoInteractions();
    then(cacheRepository).should().saveFallbackPhoto(
        "paris",
        CachedPhoto.of(UNSPLASH_RAW, "홍길동", "https://unsplash.com/@hong?utm_source=buddys"),
        FALLBACK_TTL);
    assertThat(meterRegistry.counter("place.photo.google.quota.exceeded").count()).isEqualTo(1);
  }

  @DisplayName("구글에 사진이 없으면 '사진 없음'을 캐시하고 Unsplash로 넘어간다")
  @Test
  void getPhoto_googleHasNoPhoto_cachesNotFoundAndFallsBack() {
    given(googlePlacesClient.getPlaceDetails(PLACE_ID)).willReturn(new GooglePlaceDetailsResponse(List.of()));
    givenUnsplashKeyword("Paris");
    given(cacheRepository.findFallbackPhoto("paris"))
        .willReturn(Optional.of(CachedPhoto.of(UNSPLASH_RAW, "홍길동", "https://unsplash.com/@hong")));

    PlacePhoto photo = service().getPhoto(PLACE_ID, WIDTH);

    assertThat(photo.source()).isEqualTo(PlacePhotoSource.UNSPLASH);
    then(cacheRepository).should().saveGooglePhoto(PLACE_ID, WIDTH, CachedPhoto.notFound(), NOT_FOUND_TTL);
    then(unsplashClient).shouldHaveNoInteractions();
  }

  @DisplayName("구글 '사진 없음' 캐시가 있으면 구글을 다시 부르지 않는다")
  @Test
  void getPhoto_googleNotFoundCached_skipsGoogle() {
    given(cacheRepository.findGooglePhoto(PLACE_ID, WIDTH)).willReturn(Optional.of(CachedPhoto.notFound()));

    assertThatThrownBy(() -> service().getPhoto(PLACE_ID, WIDTH))
        .isInstanceOf(BaseException.class);
    then(googlePlacesClient).shouldHaveNoInteractions();
    then(cacheRepository).should(never()).tryAcquireGoogleQuota(any(), anyInt());
  }

  @DisplayName("구글 호출이 실패하면 '사진 없음'으로 캐시하지 않고 대체 사진으로 넘어간다")
  @Test
  void getPhoto_googleFails_doesNotCacheNotFound() {
    given(googlePlacesClient.getPlaceDetails(PLACE_ID))
        .willThrow(new BaseException(PlaceErrorCode.GOOGLE_PLACES_UNAVAILABLE));
    givenUnsplashKeyword("Paris");
    given(unsplashClient.searchFirstLandscape("Paris"))
        .willReturn(Optional.of(new UnsplashImage(UNSPLASH_RAW, "홍길동", "https://unsplash.com/@hong")));

    PlacePhoto photo = service().getPhoto(PLACE_ID, WIDTH);

    assertThat(photo.source()).isEqualTo(PlacePhotoSource.UNSPLASH);
    then(cacheRepository).should(never()).saveGooglePhoto(anyString(), anyInt(), any(), any());
  }

  @DisplayName("Unsplash 결과가 없으면 '사진 없음'을 캐시하고 404를 던진다")
  @Test
  void getPhoto_unsplashNoResult_cachesNotFoundAndThrows() {
    given(cacheRepository.tryAcquireGoogleQuota(any(), eq(DAILY_LIMIT))).willReturn(false);
    givenUnsplashKeyword("Paris");
    given(unsplashClient.searchFirstLandscape("Paris")).willReturn(Optional.empty());

    assertThatThrownBy(() -> service().getPhoto(PLACE_ID, WIDTH))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(PlaceErrorCode.PLACE_PHOTO_NOT_FOUND);
    then(cacheRepository).should().saveFallbackPhoto("paris", CachedPhoto.notFound(), NOT_FOUND_TTL);
    assertThat(meterRegistry.counter("place.photo.resolved", "source", "NONE").count()).isEqualTo(1);
  }

  @DisplayName("Unsplash 호출이 실패하면 캐시하지 않고 404를 던진다")
  @Test
  void getPhoto_unsplashFails_throwsWithoutCaching() {
    given(cacheRepository.tryAcquireGoogleQuota(any(), eq(DAILY_LIMIT))).willReturn(false);
    givenUnsplashKeyword("Paris");
    given(unsplashClient.searchFirstLandscape("Paris")).willThrow(new RestClientException("429"));

    assertThatThrownBy(() -> service().getPhoto(PLACE_ID, WIDTH)).isInstanceOf(BaseException.class);
    then(cacheRepository).should(never()).saveFallbackPhoto(anyString(), any(), any());
  }

  @DisplayName("Unsplash 키가 없거나 장소 위치 정보가 없으면 Unsplash를 부르지 않는다")
  @Test
  void getPhoto_unsplashUnavailable_throwsNotFound() {
    given(cacheRepository.tryAcquireGoogleQuota(any(), eq(DAILY_LIMIT))).willReturn(false);
    given(placeRepository.findPhotoSearchKeyword(PLACE_ID)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service(false).getPhoto(PLACE_ID, WIDTH)).isInstanceOf(BaseException.class);
    assertThatThrownBy(() -> service(true).getPhoto(PLACE_ID, WIDTH)).isInstanceOf(BaseException.class);
    then(unsplashClient).shouldHaveNoInteractions();
  }

  @DisplayName("업로드 경로가 아닌 사용자 사진 URL은 건너뛰고 다음 후보를 쓴다")
  @Test
  void getPhoto_userPhotoNotUploadedUrl_skipsToNextCandidate() {
    given(cacheRepository.tryAcquireGoogleQuota(any(), eq(DAILY_LIMIT))).willReturn(false);
    given(courseImageRepository.findImageUrlsOfSinglePlaceDays(eq(PLACE_ID), any(Pageable.class)))
        .willReturn(List.of("https://evil.example.com/phishing.jpg", USER_PHOTO_URL));

    PlacePhoto photo = service().getPhoto(PLACE_ID, WIDTH);

    assertThat(photo).isEqualTo(PlacePhoto.user(USER_PHOTO_URL));
  }

  @DisplayName("업로드 경로가 아닌 사용자 사진만 있으면 Unsplash로 넘어간다")
  @Test
  void getPhoto_onlyExternalUserPhotos_fallsBackToUnsplash() {
    given(cacheRepository.tryAcquireGoogleQuota(any(), eq(DAILY_LIMIT))).willReturn(false);
    given(courseImageRepository.findImageUrlsOfSinglePlaceDays(eq(PLACE_ID), any(Pageable.class)))
        .willReturn(List.of("https://example.com/a.jpg", COURSE_UPLOAD_PREFIX + "../profiles/x.jpg"));
    givenUnsplashKeyword("Paris");
    given(unsplashClient.searchFirstLandscape("Paris"))
        .willReturn(Optional.of(new UnsplashImage(UNSPLASH_RAW, "Author", "https://unsplash.com/@author")));

    PlacePhoto photo = service().getPhoto(PLACE_ID, WIDTH);

    assertThat(photo.source()).isEqualTo(PlacePhotoSource.UNSPLASH);
  }

  @DisplayName("구글 사진 URI를 리다이렉트할 수 없으면 사용자 사진으로 넘어간다")
  @Test
  void getPhoto_googleUriNotRedirectable_fallsBackToUserPhoto() {
    givenGooglePhoto("https://google/has space");
    given(courseImageRepository.findImageUrlsOfSinglePlaceDays(eq(PLACE_ID), any(Pageable.class)))
        .willReturn(List.of(USER_PHOTO_URL));

    PlacePhoto photo = service().getPhoto(PLACE_ID, WIDTH);

    assertThat(photo).isEqualTo(PlacePhoto.user(USER_PHOTO_URL));
  }

  @DisplayName("https가 아닌 구글 사진 URI는 쓰지 않는다")
  @Test
  void getPhoto_googleUriNotHttps_skipsGoogle() {
    givenGooglePhoto("javascript:alert(1)");

    assertThatThrownBy(() -> service(false).getPhoto(PLACE_ID, WIDTH))
        .isInstanceOfSatisfying(BaseException.class, e ->
            assertThat(e.getErrorCode()).isEqualTo(PlaceErrorCode.PLACE_PHOTO_NOT_FOUND));
  }

  @DisplayName("Unsplash 사진 URL을 리다이렉트할 수 없으면 404를 던진다")
  @Test
  void getPhoto_unsplashUrlNotRedirectable_throwsNotFound() {
    given(cacheRepository.tryAcquireGoogleQuota(any(), eq(DAILY_LIMIT))).willReturn(false);
    givenUnsplashKeyword("Paris");
    given(unsplashClient.searchFirstLandscape("Paris"))
        .willReturn(Optional.of(new UnsplashImage("https://images.unsplash.com/has space", "Author", "https://unsplash.com/@a")));

    assertThatThrownBy(() -> service().getPhoto(PLACE_ID, WIDTH))
        .isInstanceOfSatisfying(BaseException.class, e ->
            assertThat(e.getErrorCode()).isEqualTo(PlaceErrorCode.PLACE_PHOTO_NOT_FOUND));
  }

  @DisplayName("다른 요청이 같은 장소를 구글로 조회 중이면 한도를 쓰지 않고 대체 사진으로 넘어간다")
  @Test
  void getPhoto_googleFetchInProgress_skipsGoogleWithoutQuota() {
    given(cacheRepository.tryLockGoogleFetch(anyString(), anyInt(), any())).willReturn(Optional.empty());
    given(courseImageRepository.findImageUrlsOfSinglePlaceDays(eq(PLACE_ID), any(Pageable.class)))
        .willReturn(List.of(USER_PHOTO_URL));

    PlacePhoto photo = service().getPhoto(PLACE_ID, WIDTH);

    assertThat(photo).isEqualTo(PlacePhoto.user(USER_PHOTO_URL));
    then(cacheRepository).should(never()).tryAcquireGoogleQuota(any(), anyInt());
    then(googlePlacesClient).shouldHaveNoInteractions();
    then(cacheRepository).should(never()).releaseGoogleFetchLock(anyString(), anyInt(), anyString());
  }

  @DisplayName("락을 잡은 뒤 캐시가 채워져 있으면 구글을 호출하지 않고 캐시를 쓴다")
  @Test
  void getPhoto_cacheFilledWhileLocking_usesCache() {
    given(cacheRepository.findGooglePhoto(PLACE_ID, WIDTH))
        .willReturn(Optional.empty())
        .willReturn(Optional.of(CachedPhoto.of("https://google/filled")));

    PlacePhoto photo = service().getPhoto(PLACE_ID, WIDTH);

    assertThat(photo).isEqualTo(PlacePhoto.google("https://google/filled"));
    then(cacheRepository).should(never()).tryAcquireGoogleQuota(any(), anyInt());
    then(googlePlacesClient).shouldHaveNoInteractions();
    then(cacheRepository).should().releaseGoogleFetchLock(PLACE_ID, WIDTH, LOCK_TOKEN);
  }

  @DisplayName("구글 조회가 성공하든 실패하든 락을 해제한다")
  @Test
  void getPhoto_googleFetch_releasesLockOnSuccessAndFailure() {
    givenGooglePhoto("https://google/new");
    service().getPhoto(PLACE_ID, WIDTH);
    then(cacheRepository).should().releaseGoogleFetchLock(PLACE_ID, WIDTH, LOCK_TOKEN);

    given(googlePlacesClient.getPlaceDetails(PLACE_ID))
        .willThrow(new BaseException(PlaceErrorCode.GOOGLE_PLACES_UNAVAILABLE));
    given(courseImageRepository.findImageUrlsOfSinglePlaceDays(eq(PLACE_ID), any(Pageable.class)))
        .willReturn(List.of(USER_PHOTO_URL));
    service().getPhoto(PLACE_ID, WIDTH);
    then(cacheRepository).should(org.mockito.Mockito.times(2)).releaseGoogleFetchLock(PLACE_ID, WIDTH, LOCK_TOKEN);
  }
}
