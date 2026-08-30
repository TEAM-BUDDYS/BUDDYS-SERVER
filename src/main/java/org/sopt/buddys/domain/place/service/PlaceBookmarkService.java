package org.sopt.buddys.domain.place.service;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.place.client.GooglePlacesClient;
import org.sopt.buddys.domain.place.client.dto.GoogleLatLng;
import org.sopt.buddys.domain.place.client.dto.GooglePlace;
import org.sopt.buddys.domain.place.code.PlaceErrorCode;
import org.sopt.buddys.domain.place.entity.Place;
import org.sopt.buddys.domain.place.entity.PlaceCategory;
import org.sopt.buddys.domain.place.entity.PlaceCategoryMapper;
import org.sopt.buddys.domain.place.repository.PlaceRepository;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장소 저장/취소 오케스트레이션. 구글 Places 호출을 DB 트랜잭션 밖에서 수행하고,
 * 실제 DB 쓰기는 {@link PlaceBookmarkTransactionService}에 위임한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class PlaceBookmarkService {

  private static final int MAX_NAME_LENGTH = 255;

  private final GooglePlacesClient googlePlacesClient;
  private final PlaceRepository placeRepository;
  private final PlaceBookmarkTransactionService placeBookmarkTransactionService;

  public void bookmark(Long userId, String googlePlaceId) {
    Place place = resolvePlace(googlePlaceId);
    placeBookmarkTransactionService.saveBookmark(userId, place.getId());
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
