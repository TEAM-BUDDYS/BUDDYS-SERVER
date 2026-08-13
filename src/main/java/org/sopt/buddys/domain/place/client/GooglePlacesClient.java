package org.sopt.buddys.domain.place.client;

import java.math.BigDecimal;
import java.net.URI;
import java.util.function.Supplier;
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

    return execute(
        () -> restTemplate.exchange(
            searchTextUrl,
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            GooglePlacesSearchResponse.class
        ).getBody(),
        "searchText query=" + query
    );
  }

  public GooglePlaceDetailsResponse getPlaceDetails(String placeId) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Goog-Api-Key", apiKey);
    headers.set("X-Goog-FieldMask", "photos");

    return execute(() -> {
      URI uri = UriComponentsBuilder.fromUriString(baseUrl + "/places/" + placeId).build().toUri();
      return restTemplate.exchange(
          uri,
          HttpMethod.GET,
          new HttpEntity<>(headers),
          GooglePlaceDetailsResponse.class
      ).getBody();
    }, "getPlaceDetails placeId=" + placeId);
  }

  public String getPhotoMediaUri(String photoName, int maxWidthPx) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Goog-Api-Key", apiKey);

    GooglePhotoMediaResponse response = execute(() -> {
      URI uri = UriComponentsBuilder.fromUriString(baseUrl + "/" + photoName + "/media")
          .queryParam("maxWidthPx", maxWidthPx)
          .queryParam("skipHttpRedirect", true)
          .build()
          .toUri();
      return restTemplate.exchange(
          uri,
          HttpMethod.GET,
          new HttpEntity<>(headers),
          GooglePhotoMediaResponse.class
      ).getBody();
    }, "getPhotoMediaUri photoName=" + photoName);

    return response != null ? response.photoUri() : null;
  }

  private <T> T execute(Supplier<T> request, String context) {
    try {
      return request.get();
    } catch (IllegalStateException e) {
      log.warn("[GooglePlacesClient] 잘못된 요청 파라미터 → {}", context, e);
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST, e);
    } catch (RestClientException e) {
      log.warn("[GooglePlacesClient] 구글 API 호출 실패 → {}", context, e);
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