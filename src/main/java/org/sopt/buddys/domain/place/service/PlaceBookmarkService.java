package org.sopt.buddys.domain.place.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.place.client.GooglePlacesClient;
import org.sopt.buddys.domain.place.client.dto.GoogleLatLng;
import org.sopt.buddys.domain.place.client.dto.GooglePlace;
import org.sopt.buddys.domain.place.code.PlaceErrorCode;
import org.sopt.buddys.domain.place.entity.Place;
import org.sopt.buddys.domain.place.entity.PlaceBookmark;
import org.sopt.buddys.domain.place.entity.PlaceCategory;
import org.sopt.buddys.domain.place.entity.PlaceCategoryMapper;
import org.sopt.buddys.domain.place.repository.PlaceBookmarkRepository;
import org.sopt.buddys.domain.place.repository.PlaceRepository;
import org.sopt.buddys.domain.place.service.result.BookmarkedPlaceListResult;
import org.sopt.buddys.domain.place.service.result.BookmarkedPlaceMarkersResult;
import org.sopt.buddys.domain.place.service.result.BookmarkedPlaceResult;
import org.sopt.buddys.domain.place.util.GoogleMapsUrlBuilder;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.sopt.buddys.global.common.PageConstants.MAX_PAGE_SIZE;

/**
 * 장소 저장/취소 오케스트레이션. 구글 Places 호출을 DB 트랜잭션 밖에서 수행하고,
 * 실제 DB 쓰기는 {@link PlaceBookmarkTransactionService}에 위임한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class PlaceBookmarkService {

  private static final int MAX_NAME_LENGTH = 255;
  private static final int MAX_MARKERS = 300;
  private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
  private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
  private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
  private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);
  private static final String PHOTO_URL_TEMPLATE = "/api/v1/places/%s/photo?maxWidth=400";

  private final GooglePlacesClient googlePlacesClient;
  private final PlaceRepository placeRepository;
  private final PlaceBookmarkRepository placeBookmarkRepository;
  private final PlaceBookmarkTransactionService placeBookmarkTransactionService;

  public void bookmark(Long userId, String googlePlaceId) {
    Place place = resolvePlace(googlePlaceId);
    placeBookmarkTransactionService.saveBookmark(userId, place.getId());
  }

  @Transactional(readOnly = true)
  public BookmarkedPlaceListResult getBookmarkedPlaces(Long userId, int page, int size) {
    validatePageRequest(page, size);

    Slice<PlaceBookmark> slice = placeBookmarkRepository
        .findAllByUserIdWithPlaceOrderByCreatedAtDesc(userId, PageRequest.of(page, size));

    List<BookmarkedPlaceResult> places = slice.getContent().stream()
        .map(this::toBookmarkedPlaceResult)
        .toList();

    return new BookmarkedPlaceListResult(places, slice.getNumber(), slice.getSize(), slice.hasNext());
  }

  @Transactional(readOnly = true)
  public BookmarkedPlaceMarkersResult getBookmarkedPlaceMarkers(
      Long userId,
      BigDecimal swLat,
      BigDecimal swLng,
      BigDecimal neLat,
      BigDecimal neLng
  ) {
    validateBounds(swLat, swLng, neLat, neLng);

    List<PlaceBookmark> bookmarks = placeBookmarkRepository.findMarkersByUserIdWithinBounds(
        userId, swLat, neLat, swLng, neLng, PageRequest.of(0, MAX_MARKERS + 1));

    boolean truncated = bookmarks.size() > MAX_MARKERS;
    List<BookmarkedPlaceResult> places = bookmarks.stream()
        .limit(MAX_MARKERS)
        .map(this::toBookmarkedPlaceResult)
        .toList();

    return new BookmarkedPlaceMarkersResult(places, truncated);
  }

  private void validateBounds(BigDecimal swLat, BigDecimal swLng, BigDecimal neLat, BigDecimal neLng) {
    boolean outOfRange = swLat.compareTo(MIN_LATITUDE) < 0 || neLat.compareTo(MAX_LATITUDE) > 0
        || swLng.compareTo(MIN_LONGITUDE) < 0 || neLng.compareTo(MAX_LONGITUDE) > 0;
    boolean misordered = swLat.compareTo(neLat) > 0 || swLng.compareTo(neLng) > 0;
    if (outOfRange || misordered) {
      throw new BaseException(PlaceErrorCode.INVALID_MAP_BOUNDS);
    }
  }

  private BookmarkedPlaceResult toBookmarkedPlaceResult(PlaceBookmark bookmark) {
    Place place = bookmark.getPlace();
    return new BookmarkedPlaceResult(
        place.getGooglePlaceId(),
        place.getName(),
        place.getCategory(),
        place.getAddress(),
        toDouble(place.getLatitude()),
        toDouble(place.getLongitude()),
        PHOTO_URL_TEMPLATE.formatted(place.getGooglePlaceId()),
        GoogleMapsUrlBuilder.toPlaceUrl(
            place.getGooglePlaceId(),
            place.getName() != null ? place.getName() : place.getAddress()
        ),
        bookmark.getCreatedAt()
    );
  }

  private void validatePageRequest(int page, int size) {
    if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }

  private Double toDouble(BigDecimal value) {
    return value == null ? null : value.doubleValue();
  }

  public void cancelBookmark(Long userId, String googlePlaceId) {
    placeRepository.findByGooglePlaceId(googlePlaceId)
        .ifPresent(place -> placeBookmarkTransactionService.deleteBookmark(userId, place.getId()));
  }

  private Place resolvePlace(String googlePlaceId) {
    return placeRepository.findByGooglePlaceId(googlePlaceId)
        .orElseGet(() -> createPlaceFromGoogle(googlePlaceId));
  }

  private Place createPlaceFromGoogle(String googlePlaceId) {
    GooglePlace google = googlePlacesClient.getPlace(googlePlaceId);
    Place place = Place.builder()
        .googlePlaceId(googlePlaceId)
        .name(resolveName(google))
        .category(PlaceCategoryMapper.resolveCategory(google.primaryType(), google.types())
            .orElse(PlaceCategory.ETC))
        .address(google.formattedAddress())
        .latitude(toBigDecimal(google.location(), true))
        .longitude(toBigDecimal(google.location(), false))
        .build();

    try {
      return placeBookmarkTransactionService.savePlace(place);
    } catch (DataIntegrityViolationException e) {
      // 동시에 같은 장소를 저장한 요청이 이미 캐시 행을 만든 경우
      return placeRepository.findByGooglePlaceId(googlePlaceId).orElseThrow(() -> e);
    }
  }

  private String resolveName(GooglePlace google) {
    String name = google.displayName() != null ? google.displayName().text() : null;
    if (name == null || name.isBlank()) {
      name = google.formattedAddress();
    }
    if (name == null || name.isBlank()) {
      throw new BaseException(PlaceErrorCode.GOOGLE_PLACES_UNAVAILABLE);
    }
    return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
  }

  private BigDecimal toBigDecimal(GoogleLatLng location, boolean latitude) {
    if (location == null) {
      return null;
    }
    Double value = latitude ? location.latitude() : location.longitude();
    return value == null ? null : BigDecimal.valueOf(value);
  }
}
