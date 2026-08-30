package org.sopt.buddys.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.place.client.GooglePlacesClient;
import org.sopt.buddys.domain.place.client.dto.GoogleDisplayName;
import org.sopt.buddys.domain.place.client.dto.GoogleLatLng;
import org.sopt.buddys.domain.place.client.dto.GooglePlace;
import org.sopt.buddys.domain.place.code.PlaceErrorCode;
import org.sopt.buddys.domain.place.entity.Place;
import org.sopt.buddys.domain.place.entity.PlaceBookmark;
import org.sopt.buddys.domain.place.entity.PlaceCategory;
import org.sopt.buddys.domain.place.repository.PlaceBookmarkRepository;
import org.sopt.buddys.domain.place.repository.PlaceRepository;
import org.sopt.buddys.domain.place.service.result.BookmarkedPlaceListResult;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlaceBookmarkServiceTest {

  private static final String GOOGLE_PLACE_ID = "ChIJN1t_tDeuEmsRUsoyG83frY4";

  @InjectMocks
  private PlaceBookmarkService placeBookmarkService;

  @Mock
  private GooglePlacesClient googlePlacesClient;

  @Mock
  private PlaceRepository placeRepository;

  @Mock
  private PlaceBookmarkRepository placeBookmarkRepository;

  @Mock
  private PlaceBookmarkTransactionService placeBookmarkTransactionService;

  @DisplayName("이미 캐시된 장소면 구글을 호출하지 않고 저장한다")
  @Test
  void bookmark_existingPlace_doesNotCallGoogle() {
    // given
    Place place = place(10L);
    given(placeRepository.findByGooglePlaceId(GOOGLE_PLACE_ID)).willReturn(Optional.of(place));

    // when
    placeBookmarkService.bookmark(1L, GOOGLE_PLACE_ID);

    // then
    then(googlePlacesClient).shouldHaveNoInteractions();
    then(placeBookmarkTransactionService).should().saveBookmark(1L, 10L);
  }

  @DisplayName("캐시에 없으면 구글 상세 조회로 장소를 만들어 저장한다")
  @Test
  void bookmark_newPlace_createsFromGoogle() {
    // given
    given(placeRepository.findByGooglePlaceId(GOOGLE_PLACE_ID)).willReturn(Optional.empty());
    given(googlePlacesClient.getPlace(GOOGLE_PLACE_ID)).willReturn(new GooglePlace(
        GOOGLE_PLACE_ID,
        new GoogleDisplayName("루브르 박물관", "ko"),
        "art_gallery",
        List.of("art_gallery", "tourist_attraction"),
        "Rue de Rivoli, 75001 Paris",
        new GoogleLatLng(48.8606, 2.3376),
        null
    ));
    given(placeBookmarkTransactionService.savePlace(any(Place.class))).willReturn(place(20L));

    // when
    placeBookmarkService.bookmark(1L, GOOGLE_PLACE_ID);

    // then
    ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);
    then(placeBookmarkTransactionService).should().savePlace(captor.capture());
    assertThat(captor.getValue()).satisfies(saved -> {
      assertThat(saved.getGooglePlaceId()).isEqualTo(GOOGLE_PLACE_ID);
      assertThat(saved.getName()).isEqualTo("루브르 박물관");
      assertThat(saved.getCategory()).isEqualTo(PlaceCategory.TOURISM);
      assertThat(saved.getAddress()).isEqualTo("Rue de Rivoli, 75001 Paris");
      assertThat(saved.getLatitude()).isEqualByComparingTo("48.8606");
      assertThat(saved.getLongitude()).isEqualByComparingTo("2.3376");
    });
    then(placeBookmarkTransactionService).should().saveBookmark(1L, 20L);
  }

  @DisplayName("장소 캐시 저장이 동시성으로 실패하면 이미 저장된 행을 재조회해 이어간다")
  @Test
  void bookmark_placeInsertRace_recoversByRequery() {
    // given
    given(placeRepository.findByGooglePlaceId(GOOGLE_PLACE_ID))
        .willReturn(Optional.empty())
        .willReturn(Optional.of(place(30L)));
    given(googlePlacesClient.getPlace(GOOGLE_PLACE_ID)).willReturn(googlePlace());
    given(placeBookmarkTransactionService.savePlace(any(Place.class)))
        .willThrow(new DataIntegrityViolationException("uk_place_google_place_id"));

    // when
    placeBookmarkService.bookmark(1L, GOOGLE_PLACE_ID);

    // then
    then(placeBookmarkTransactionService).should().saveBookmark(1L, 30L);
  }

  @DisplayName("구글이 이름도 주소도 주지 않으면 저장하지 않고 예외를 던진다")
  @Test
  void bookmark_googleReturnsNoNameNorAddress_throws() {
    // given
    given(placeRepository.findByGooglePlaceId(GOOGLE_PLACE_ID)).willReturn(Optional.empty());
    given(googlePlacesClient.getPlace(GOOGLE_PLACE_ID)).willReturn(new GooglePlace(
        GOOGLE_PLACE_ID, null, "restaurant", List.of("restaurant"), null, null, null));

    // when & then
    assertThatThrownBy(() -> placeBookmarkService.bookmark(1L, GOOGLE_PLACE_ID))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(PlaceErrorCode.GOOGLE_PLACES_UNAVAILABLE);
    then(placeBookmarkTransactionService).should(never()).savePlace(any());
    then(placeBookmarkTransactionService).should(never()).saveBookmark(anyLong(), anyLong());
  }

  @DisplayName("이름이 없으면 formattedAddress를 이름으로 사용한다")
  @Test
  void bookmark_missingName_fallsBackToAddress() {
    // given
    given(placeRepository.findByGooglePlaceId(GOOGLE_PLACE_ID)).willReturn(Optional.empty());
    given(googlePlacesClient.getPlace(GOOGLE_PLACE_ID)).willReturn(new GooglePlace(
        GOOGLE_PLACE_ID, null, "cafe", List.of("cafe"), "서울特別市 중구 세종대로 110", null, null));
    given(placeBookmarkTransactionService.savePlace(any(Place.class))).willReturn(place(40L));

    // when
    placeBookmarkService.bookmark(1L, GOOGLE_PLACE_ID);

    // then
    ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);
    then(placeBookmarkTransactionService).should().savePlace(captor.capture());
    assertThat(captor.getValue().getName()).isEqualTo("서울特別市 중구 세종대로 110");
  }

  @DisplayName("저장 취소는 캐시에 장소가 있으면 북마크만 삭제한다")
  @Test
  void cancelBookmark_existingPlace_deletesBookmark() {
    // given
    given(placeRepository.findByGooglePlaceId(GOOGLE_PLACE_ID)).willReturn(Optional.of(place(50L)));

    // when
    placeBookmarkService.cancelBookmark(1L, GOOGLE_PLACE_ID);

    // then
    then(placeBookmarkTransactionService).should().deleteBookmark(1L, 50L);
  }

  @DisplayName("저장 취소 시 캐시에 장소가 없으면 아무 것도 하지 않는다")
  @Test
  void cancelBookmark_unknownPlace_noOp() {
    // given
    given(placeRepository.findByGooglePlaceId(GOOGLE_PLACE_ID)).willReturn(Optional.empty());

    // when
    placeBookmarkService.cancelBookmark(1L, GOOGLE_PLACE_ID);

    // then
    then(placeBookmarkTransactionService).should(never()).deleteBookmark(anyLong(), anyLong());
  }

  @DisplayName("저장한 장소를 최근 저장순 스냅샷으로 매핑해 반환한다")
  @Test
  void getBookmarkedPlaces_mapsSnapshotAndPaging() {
    // given
    Place louvre = Place.builder()
        .id(1L)
        .googlePlaceId("place-louvre")
        .name("루브르 박물관")
        .category(PlaceCategory.TOURISM)
        .address("Rue de Rivoli, 75001 Paris")
        .latitude(new BigDecimal("48.8606000"))
        .longitude(new BigDecimal("2.3376000"))
        .build();
    PlaceBookmark bookmark = bookmark(louvre, LocalDateTime.of(2026, 8, 30, 21, 0));
    given(placeBookmarkRepository.findAllByUserIdWithPlaceOrderByCreatedAtDesc(1L, PageRequest.of(0, 20)))
        .willReturn(new SliceImpl<>(List.of(bookmark), PageRequest.of(0, 20), true));

    // when
    BookmarkedPlaceListResult result = placeBookmarkService.getBookmarkedPlaces(1L, 0, 20);

    // then
    assertThat(result.page()).isZero();
    assertThat(result.size()).isEqualTo(20);
    assertThat(result.hasNext()).isTrue();
    assertThat(result.places()).singleElement().satisfies(place -> {
      assertThat(place.placeId()).isEqualTo("place-louvre");
      assertThat(place.name()).isEqualTo("루브르 박물관");
      assertThat(place.category()).isEqualTo(PlaceCategory.TOURISM);
      assertThat(place.latitude()).isEqualTo(48.8606);
      assertThat(place.longitude()).isEqualTo(2.3376);
      assertThat(place.photoUrl()).isEqualTo("/api/v1/places/place-louvre/photo?maxWidth=400");
      assertThat(place.googleMapsUrl()).isEqualTo(
          "https://www.google.com/maps/search/?api=1"
              + "&query=%EB%A3%A8%EB%B8%8C%EB%A5%B4+%EB%B0%95%EB%AC%BC%EA%B4%80"
              + "&query_place_id=place-louvre");
      assertThat(place.bookmarkedAt()).isEqualTo(LocalDateTime.of(2026, 8, 30, 21, 0));
    });
  }

  @DisplayName("size가 허용 범위를 벗어나면 조회하지 않고 예외가 발생한다")
  @Test
  void getBookmarkedPlaces_invalidSize_throws() {
    // when & then
    assertThatThrownBy(() -> placeBookmarkService.getBookmarkedPlaces(1L, 0, 101))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(GlobalErrorCode.INVALID_REQUEST);
    then(placeBookmarkRepository).shouldHaveNoInteractions();
  }

  private static PlaceBookmark bookmark(Place place, LocalDateTime bookmarkedAt) {
    User user = mock(User.class);
    given(user.getId()).willReturn(1L);
    PlaceBookmark bookmark = new PlaceBookmark(user, place);
    ReflectionTestUtils.setField(bookmark, "createdAt", bookmarkedAt);
    return bookmark;
  }

  private static GooglePlace googlePlace() {
    return new GooglePlace(
        GOOGLE_PLACE_ID,
        new GoogleDisplayName("어떤 장소", "ko"),
        "restaurant",
        List.of("restaurant"),
        "어딘가",
        new GoogleLatLng(37.5, 127.0),
        null
    );
  }

  private static Place place(Long id) {
    return Place.builder()
        .id(id)
        .googlePlaceId(GOOGLE_PLACE_ID)
        .name("장소")
        .category(PlaceCategory.ETC)
        .latitude(BigDecimal.ONE)
        .longitude(BigDecimal.ONE)
        .build();
  }
}
