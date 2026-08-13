package org.sopt.buddys.domain.place.client;

import java.math.BigDecimal;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.domain.place.client.dto.GoogleCircle;
import org.sopt.buddys.domain.place.client.dto.GoogleLatLng;
import org.sopt.buddys.domain.place.client.dto.GoogleLocationBias;
import org.sopt.buddys.domain.place.client.dto.GooglePhotoMediaResponse;
import org.sopt.buddys.domain.place.client.dto.GooglePlaceDetailsResponse;
import org.sopt.buddys.domain.place.client.dto.GooglePlacesSearchRequest;
import org.sopt.buddys.domain.place.client.dto.GooglePlacesSearchResponse;
import org.sopt.buddys.domain.place.code.PlaceErrorCode;
import org.sopt.buddys.domain.place.entity.PlaceCategory;
import org.sopt.buddys.domain.place.entity.PlaceCategoryMapper;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class GooglePlacesClient {

  private static final String FIELD_MASK =
      "places.id,places.displayName,places.primaryType,places.formattedAddress,places.location,places.photos,nextPageToken";
  private static final double DEFAULT_BIAS_RADIUS_METERS = 20_000.0;

  private final RestTemplate restTemplate;

  @Value("${google.places.api-key}")
  private String apiKey;

  @Value("${google.places.search-text-url}")
  private String searchTextUrl;

  @Value("${google.places.base-url}")
  private String baseUrl;

  public GooglePlacesSearchResponse searchText(
      String query,
      PlaceCategory category,
      BigDecimal lat,
      BigDecimal lng,
      String pageToken
  ) {
    GooglePlacesSearchRequest request = new GooglePlacesSearchRequest(
        query,
        PlaceCategoryMapper.toGoogleIncludedType(category),
        toLocationBias(lat, lng),
        pageToken
    );

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Goog-Api-Key", apiKey);
    headers.set("X-Goog-FieldMask", FIELD_MASK);

    try {
      return restTemplate.exchange(
          searchTextUrl,
          HttpMethod.POST,
          new HttpEntity<>(request, headers),
          GooglePlacesSearchResponse.class
      ).getBody();
    } catch (RestClientException e) {
      log.warn("[GooglePlacesClient] searchText 호출 실패 → query={}", query, e);
      throw new BaseException(PlaceErrorCode.GOOGLE_PLACES_UNAVAILABLE, e);
    }
  }

  public GooglePlaceDetailsResponse getPlaceDetails(String placeId) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Goog-Api-Key", apiKey);
    headers.set("X-Goog-FieldMask", "photos");

    try {
      URI uri = UriComponentsBuilder.fromUriString(baseUrl + "/places/" + placeId).build().toUri();
      return restTemplate.exchange(
          uri,
          HttpMethod.GET,
          new HttpEntity<>(headers),
          GooglePlaceDetailsResponse.class
      ).getBody();
    } catch (IllegalStateException e) {
      log.warn("[GooglePlacesClient] 잘못된 placeId → placeId={}", placeId, e);
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST, e);
    } catch (RestClientException e) {
      log.warn("[GooglePlacesClient] getPlaceDetails 호출 실패 → placeId={}", placeId, e);
      throw new BaseException(PlaceErrorCode.GOOGLE_PLACES_UNAVAILABLE, e);
    }
  }

  public String getPhotoMediaUri(String photoName, int maxWidthPx) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Goog-Api-Key", apiKey);

    try {
      URI uri = UriComponentsBuilder.fromUriString(baseUrl + "/" + photoName + "/media")
          .queryParam("maxWidthPx", maxWidthPx)
          .queryParam("skipHttpRedirect", true)
          .build()
          .toUri();
      GooglePhotoMediaResponse response = restTemplate.exchange(
          uri,
          HttpMethod.GET,
          new HttpEntity<>(headers),
          GooglePhotoMediaResponse.class
      ).getBody();
      return response != null ? response.photoUri() : null;
    } catch (IllegalStateException e) {
      log.warn("[GooglePlacesClient] 잘못된 photoName → photoName={}", photoName, e);
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST, e);
    } catch (RestClientException e) {
      log.warn("[GooglePlacesClient] getPhotoMediaUri 호출 실패 → photoName={}", photoName, e);
      throw new BaseException(PlaceErrorCode.GOOGLE_PLACES_UNAVAILABLE, e);
    }
  }

  private GoogleLocationBias toLocationBias(BigDecimal lat, BigDecimal lng) {
    if (lat == null || lng == null) {
      return null;
    }
    return new GoogleLocationBias(
        new GoogleCircle(new GoogleLatLng(lat.doubleValue(), lng.doubleValue()), DEFAULT_BIAS_RADIUS_METERS)
    );
  }
}