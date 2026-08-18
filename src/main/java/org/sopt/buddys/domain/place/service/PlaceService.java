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
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

  private static final String PHOTO_URL_TEMPLATE = "/api/v1/places/%s/photo?maxWidth=400";
  private static final int DEFAULT_NEARBY_RADIUS_METERS = 1_500;
  private static final int MIN_NEARBY_RADIUS_METERS = 1;
  private static final int MAX_NEARBY_RADIUS_METERS = 50_000;
  private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
  private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
  private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
  private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);

  private final GooglePlacesClient googlePlacesClient;
  private final PlaceBookmarkRepository placeBookmarkRepository;

  public PlaceSearchResult search(
      Long userId,
      String query,
      String categoryRaw,
      BigDecimal lat,
      BigDecimal lng,
      String pageToken
  ) {
    validateQuery(query);
    validateCoordinates(lat, lng);
    validateCoordinateRange(lat, lng);
    PlaceCategory category = parseCategory(categoryRaw);

    var response = googlePlacesClient.searchText(query.trim(), lat, lng, pageToken);

    return toSearchResult(userId, response.placesOrEmpty(), category, response.nextPageToken());
  }

  public PlaceSearchResult nearby(
      Long userId,
      BigDecimal lat,
      BigDecimal lng,
      Integer radiusMeters,
      String categoryRaw
  ) {
    validateRequiredCoordinates(lat, lng);
    validateCoordinateRange(lat, lng);
    int radius = resolveRadius(radiusMeters);
    PlaceCategory category = parseCategory(categoryRaw);

    var response = googlePlacesClient.searchNearby(lat, lng, radius, category);

    return toSearchResult(userId, response.placesOrEmpty(), category, null);
  }

  private PlaceSearchResult toSearchResult(
      Long userId,
      List<GooglePlace> places,
      PlaceCategory category,
      String nextPageToken
  ) {
    List<MatchedPlace> matched = places.stream()
        .map(place -> new MatchedPlace(place,
            PlaceCategoryMapper.resolveCategory(place.primaryType(), place.types()).orElse(null)))
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

    return new PlaceSearchResult(items, nextPageToken);
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public String getPhotoRedirectUri(String placeId, int maxWidth) {
    List<GooglePhoto> photos = googlePlacesClient.getPlaceDetails(placeId).photosOrEmpty();

    GooglePhoto firstPhoto = photos.stream()
        .findFirst()
        .orElseThrow(() -> new BaseException(PlaceErrorCode.PLACE_PHOTO_NOT_FOUND));

    return googlePlacesClient.getPhotoMediaUri(firstPhoto.name(), maxWidth);
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

  private PlaceCategory parseCategory(String categoryRaw) {
    if (categoryRaw == null || categoryRaw.isBlank()) {
      return null;
    }
    try {
      return PlaceCategory.valueOf(categoryRaw.trim());
    } catch (IllegalArgumentException e) {
      throw new BaseException(PlaceErrorCode.INVALID_CATEGORY);
    }
  }

  private void validateQuery(String query) {
    if (query == null || query.isBlank()) {
      throw new BaseException(PlaceErrorCode.MISSING_QUERY);
    }
  }

  private void validateCoordinates(BigDecimal lat, BigDecimal lng) {
    if ((lat == null) != (lng == null)) {
      throw new BaseException(PlaceErrorCode.LAT_LNG_MUST_BE_PAIRED);
    }
  }

  private void validateRequiredCoordinates(BigDecimal lat, BigDecimal lng) {
    if (lat == null || lng == null) {
      throw new BaseException(PlaceErrorCode.MISSING_COORDINATES);
    }
  }

  private void validateCoordinateRange(BigDecimal lat, BigDecimal lng) {
    if (lat == null || lng == null) {
      return;
    }
    boolean outOfRange = lat.compareTo(MIN_LATITUDE) < 0 || lat.compareTo(MAX_LATITUDE) > 0
        || lng.compareTo(MIN_LONGITUDE) < 0 || lng.compareTo(MAX_LONGITUDE) > 0;
    if (outOfRange) {
      throw new BaseException(PlaceErrorCode.INVALID_COORDINATE_RANGE);
    }
  }

  private int resolveRadius(Integer radiusMeters) {
    if (radiusMeters == null) {
      return DEFAULT_NEARBY_RADIUS_METERS;
    }
    if (radiusMeters < MIN_NEARBY_RADIUS_METERS || radiusMeters > MAX_NEARBY_RADIUS_METERS) {
      throw new BaseException(PlaceErrorCode.INVALID_RADIUS);
    }
    return radiusMeters;
  }
}
