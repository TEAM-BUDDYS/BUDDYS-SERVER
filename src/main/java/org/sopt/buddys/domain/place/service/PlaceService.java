package org.sopt.buddys.domain.place.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.place.client.GooglePlacesClient;
import org.sopt.buddys.domain.place.client.dto.GooglePhoto;
import org.sopt.buddys.domain.place.client.dto.GooglePlace;
import org.sopt.buddys.domain.place.code.PlaceErrorCode;
import org.sopt.buddys.domain.place.entity.PlaceCategory;
import org.sopt.buddys.domain.place.entity.PlaceCategoryMapper;
import org.sopt.buddys.domain.place.repository.PlaceBookmarkRepository;
import org.sopt.buddys.domain.place.service.result.PlaceSearchResult;
import org.sopt.buddys.domain.place.service.result.PlaceSearchResult.PlaceSearchItemResult;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

  private static final String PHOTO_URL_TEMPLATE = "/api/v1/places/%s/photo?maxWidth=400";

  private final GooglePlacesClient googlePlacesClient;
  private final PlaceBookmarkRepository placeBookmarkRepository;

  public PlaceSearchResult search(
      Long userId,
      String query,
      PlaceCategory category,
      BigDecimal lat,
      BigDecimal lng,
      String pageToken
  ) {
    validateQuery(query);
    validateCoordinates(lat, lng);

    var response = googlePlacesClient.searchText(query.trim(), category, lat, lng, pageToken);

    List<MatchedPlace> matched = response.placesOrEmpty().stream()
        .map(place -> new MatchedPlace(place, PlaceCategoryMapper.fromGooglePrimaryType(place.primaryType()).orElse(null)))
        .filter(matchedPlace -> matchedPlace.category() != null
            && (category == null || category == matchedPlace.category()))
        .toList();

    Set<String> bookmarkedGooglePlaceIds =
        findBookmarkedGooglePlaceIds(userId, matched.stream().map(MatchedPlace::place).toList());

    List<PlaceSearchItemResult> items = matched.stream()
        .map(matchedPlace -> toItemResult(
            matchedPlace.place(),
            matchedPlace.category(),
            bookmarkedGooglePlaceIds.contains(matchedPlace.place().id())
        ))
        .toList();

    return new PlaceSearchResult(items, response.nextPageToken());
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public String getPhotoRedirectUri(String placeId, int maxWidth) {
    List<GooglePhoto> photos = googlePlacesClient.getPlaceDetails(placeId).photosOrEmpty();

    GooglePhoto firstPhoto = photos.stream()
        .findFirst()
        .orElseThrow(() -> new BaseException(PlaceErrorCode.PLACE_PHOTO_NOT_FOUND));

    String photoUri = googlePlacesClient.getPhotoMediaUri(firstPhoto.name(), maxWidth);
    if (photoUri == null) {
      throw new BaseException(PlaceErrorCode.PLACE_PHOTO_NOT_FOUND);
    }
    return photoUri;
  }

  private Set<String> findBookmarkedGooglePlaceIds(Long userId, List<GooglePlace> places) {
    if (places.isEmpty()) {
      return Set.of();
    }
    List<String> googlePlaceIds = places.stream().map(GooglePlace::id).toList();
    return Set.copyOf(placeBookmarkRepository.findBookmarkedGooglePlaceIds(userId, googlePlaceIds));
  }

  private PlaceSearchItemResult toItemResult(GooglePlace place, PlaceCategory category, boolean bookmarked) {
    Double latitude = place.location() != null ? place.location().latitude() : null;
    Double longitude = place.location() != null ? place.location().longitude() : null;
    boolean hasPhoto = place.photos() != null && !place.photos().isEmpty();

    return new PlaceSearchItemResult(
        place.id(),
        place.displayName() != null ? place.displayName().text() : null,
        category,
        place.formattedAddress(),
        latitude,
        longitude,
        bookmarked,
        hasPhoto ? PHOTO_URL_TEMPLATE.formatted(place.id()) : null
    );
  }

  private record MatchedPlace(GooglePlace place, PlaceCategory category) {
  }

  private void validateQuery(String query) {
    if (query == null || query.isBlank()) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }

  private void validateCoordinates(BigDecimal lat, BigDecimal lng) {
    if ((lat == null) != (lng == null)) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }
}