package org.sopt.buddys.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.place.client.GooglePlacesClient;
import org.sopt.buddys.domain.place.client.dto.GoogleDisplayName;
import org.sopt.buddys.domain.place.client.dto.GoogleLatLng;
import org.sopt.buddys.domain.place.client.dto.GooglePhoto;
import org.sopt.buddys.domain.place.client.dto.GooglePlace;
import org.sopt.buddys.domain.place.client.dto.GooglePlaceDetailsResponse;
import org.sopt.buddys.domain.place.client.dto.GooglePlacesSearchResponse;
import org.sopt.buddys.domain.place.code.PlaceErrorCode;
import org.sopt.buddys.domain.place.entity.PlaceCategory;
import org.sopt.buddys.domain.place.repository.PlaceBookmarkRepository;
import org.sopt.buddys.domain.place.service.result.PlaceSearchResult;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

  @InjectMocks
  private PlaceService placeService;

  @Mock
  private GooglePlacesClient googlePlacesClient;

  @Mock
  private PlaceBookmarkRepository placeBookmarkRepository;

  @DisplayName("category를 지정하면 매핑된 카테고리가 일치하는 장소만 반환한다")
  @Test
  void search_withCategory_returnsOnlyMatchingCategory() {
    // given
    GooglePlace restaurant = createGooglePlace("place-restaurant", "restaurant");
    GooglePlace museum = createGooglePlace("place-museum", "museum");
    given(googlePlacesClient.searchText("맛집", PlaceCategory.RESTAURANT, null, null, null))
        .willReturn(new GooglePlacesSearchResponse(List.of(restaurant, museum), null));
    given(placeBookmarkRepository.findBookmarkedGooglePlaceIds(1L, List.of("place-restaurant")))
        .willReturn(List.of());

    // when
    PlaceSearchResult result = placeService.search(1L, "맛집", PlaceCategory.RESTAURANT, null, null, null);

    // then
    assertThat(result.places())
        .extracting(PlaceSearchResult.PlaceSearchItemResult::placeId)
        .containsExactly("place-restaurant");
  }

  @DisplayName("category가 없으면 매핑 가능한 모든 카테고리의 장소를 반환한다")
  @Test
  void search_withoutCategory_returnsAllMappableCategories() {
    // given
    GooglePlace restaurant = createGooglePlace("place-restaurant", "restaurant");
    GooglePlace lodging = createGooglePlace("place-lodging", "lodging");
    given(googlePlacesClient.searchText(anyString(), any(), any(), any(), any()))
        .willReturn(new GooglePlacesSearchResponse(List.of(restaurant, lodging), null));
    given(placeBookmarkRepository.findBookmarkedGooglePlaceIds(any(), any())).willReturn(List.of());

    // when
    PlaceSearchResult result = placeService.search(1L, "서울", null, null, null, null);

    // then
    assertThat(result.places())
        .extracting(PlaceSearchResult.PlaceSearchItemResult::placeId)
        .containsExactlyInAnyOrder("place-restaurant", "place-lodging");
  }

  @DisplayName("우리 4종 카테고리로 매핑되지 않는 구글 타입은 결과에서 제외된다")
  @Test
  void search_unmappableGoogleType_isExcluded() {
    // given
    GooglePlace unmappable = createGooglePlace("place-unknown", "parking");
    given(googlePlacesClient.searchText(anyString(), any(), any(), any(), any()))
        .willReturn(new GooglePlacesSearchResponse(List.of(unmappable), null));

    // when
    PlaceSearchResult result = placeService.search(1L, "주차장", null, null, null, null);

    // then
    assertThat(result.places()).isEmpty();
    then(placeBookmarkRepository).should(never()).findBookmarkedGooglePlaceIds(any(), any());
  }

  @DisplayName("유저가 북마크한 장소는 bookmarked가 true로 표시된다")
  @Test
  void search_bookmarkedPlace_marksBookmarkedTrue() {
    // given
    GooglePlace bookmarked = createGooglePlace("place-bookmarked", "cafe");
    GooglePlace notBookmarked = createGooglePlace("place-plain", "cafe");
    given(googlePlacesClient.searchText(anyString(), any(), any(), any(), any()))
        .willReturn(new GooglePlacesSearchResponse(List.of(bookmarked, notBookmarked), null));
    given(placeBookmarkRepository.findBookmarkedGooglePlaceIds(1L, List.of("place-bookmarked", "place-plain")))
        .willReturn(List.of("place-bookmarked"));

    // when
    PlaceSearchResult result = placeService.search(1L, "카페", PlaceCategory.CAFE, null, null, null);

    // then
    assertThat(result.places())
        .filteredOn(item -> item.placeId().equals("place-bookmarked"))
        .extracting(PlaceSearchResult.PlaceSearchItemResult::bookmarked)
        .containsExactly(true);
    assertThat(result.places())
        .filteredOn(item -> item.placeId().equals("place-plain"))
        .extracting(PlaceSearchResult.PlaceSearchItemResult::bookmarked)
        .containsExactly(false);
  }

  @DisplayName("사진이 있는 장소는 프록시 photoUrl을 내려주고, 없으면 null이다")
  @Test
  void search_photoPresence_determinesPhotoUrl() {
    // given
    GooglePlace withPhoto = new GooglePlace(
        "place-with-photo",
        new GoogleDisplayName("사진 있는 장소", "ko"),
        "cafe",
        "서울",
        new GoogleLatLng(37.5, 127.0),
        List.of(new GooglePhoto("places/place-with-photo/photos/photo-1"))
    );
    GooglePlace withoutPhoto = createGooglePlace("place-without-photo", "cafe");
    given(googlePlacesClient.searchText(anyString(), any(), any(), any(), any()))
        .willReturn(new GooglePlacesSearchResponse(List.of(withPhoto, withoutPhoto), null));
    given(placeBookmarkRepository.findBookmarkedGooglePlaceIds(any(), any())).willReturn(List.of());

    // when
    PlaceSearchResult result = placeService.search(1L, "카페", PlaceCategory.CAFE, null, null, null);

    // then
    assertThat(result.places())
        .filteredOn(item -> item.placeId().equals("place-with-photo"))
        .extracting(PlaceSearchResult.PlaceSearchItemResult::photoUrl)
        .containsExactly("/api/v1/places/place-with-photo/photo?maxWidth=400");
    assertThat(result.places())
        .filteredOn(item -> item.placeId().equals("place-without-photo"))
        .extracting(PlaceSearchResult.PlaceSearchItemResult::photoUrl)
        .containsExactly((String) null);
  }

  @DisplayName("구글의 nextPageToken을 그대로 전달한다")
  @Test
  void search_propagatesNextPageToken() {
    // given
    given(googlePlacesClient.searchText(anyString(), any(), any(), any(), any()))
        .willReturn(new GooglePlacesSearchResponse(List.of(), "AeCrKx..."));

    // when
    PlaceSearchResult result = placeService.search(1L, "카페", null, null, null, null);

    // then
    assertThat(result.nextPageToken()).isEqualTo("AeCrKx...");
  }

  @DisplayName("검색어가 공백이면 구글 API를 호출하지 않고 예외가 발생한다")
  @Test
  void search_blankQuery_throwsWithoutCallingGoogle() {
    // when, then
    assertThatThrownBy(() -> placeService.search(1L, "   ", null, null, null, null))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(GlobalErrorCode.INVALID_REQUEST);
    then(googlePlacesClient).should(never()).searchText(any(), any(), any(), any(), any());
  }

  @DisplayName("위도만 있고 경도가 없으면 예외가 발생한다")
  @Test
  void search_onlyLatitudeGiven_throwsException() {
    // when, then
    assertThatThrownBy(() ->
        placeService.search(1L, "카페", null, BigDecimal.valueOf(37.5), null, null))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(GlobalErrorCode.INVALID_REQUEST);
    then(googlePlacesClient).should(never()).searchText(any(), any(), any(), any(), any());
  }

  @DisplayName("장소에 사진이 있으면 리다이렉트용 URI를 반환한다")
  @Test
  void getPhotoRedirectUri_photoExists_returnsUri() {
    // given
    GooglePhoto photo = new GooglePhoto("places/place-1/photos/photo-1");
    given(googlePlacesClient.getPlaceDetails("place-1"))
        .willReturn(new GooglePlaceDetailsResponse(List.of(photo)));
    given(googlePlacesClient.getPhotoMediaUri("places/place-1/photos/photo-1", 400))
        .willReturn("https://lh3.googleusercontent.com/signed-url");

    // when
    String uri = placeService.getPhotoRedirectUri("place-1", 400);

    // then
    assertThat(uri).isEqualTo("https://lh3.googleusercontent.com/signed-url");
  }

  @DisplayName("장소에 사진이 없으면 예외가 발생한다")
  @Test
  void getPhotoRedirectUri_noPhotos_throwsPhotoNotFound() {
    // given
    given(googlePlacesClient.getPlaceDetails("place-1"))
        .willReturn(new GooglePlaceDetailsResponse(List.of()));

    // when, then
    assertThatThrownBy(() -> placeService.getPhotoRedirectUri("place-1", 400))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(PlaceErrorCode.PLACE_PHOTO_NOT_FOUND);
    then(googlePlacesClient).should(never()).getPhotoMediaUri(any(), anyInt());
  }

  private GooglePlace createGooglePlace(String id, String primaryType) {
    return new GooglePlace(
        id,
        new GoogleDisplayName("이름", "ko"),
        primaryType,
        "서울",
        new GoogleLatLng(37.5, 127.0),
        List.of()
    );
  }
}