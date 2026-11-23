package org.sopt.buddys.domain.place.client;

import java.net.URI;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.place.client.dto.unsplash.UnsplashImage;
import org.sopt.buddys.domain.place.client.dto.unsplash.UnsplashPhoto;
import org.sopt.buddys.domain.place.client.dto.unsplash.UnsplashSearchResponse;
import org.sopt.buddys.domain.place.config.UnsplashProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class UnsplashClient {

  private final RestTemplate restTemplate;
  private final UnsplashProperties properties;

  /**
   * 검색어에 맞는 가로 사진 1장을 조회한다. 결과가 없으면 빈 값을 반환한다.
   *
   * @throws RestClientException Unsplash 호출에 실패한 경우 (한도 초과 포함)
   */
  public Optional<UnsplashImage> searchFirstLandscape(String query) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, "Client-ID " + properties.accessKey());
    headers.set("Accept-Version", "v1");

    URI uri = UriComponentsBuilder.fromUriString(properties.searchUrl())
        .queryParam("query", query)
        .queryParam("per_page", 1)
        .queryParam("orientation", "landscape")
        .queryParam("content_filter", "high")
        .encode()
        .build()
        .toUri();

    UnsplashSearchResponse response = restTemplate.exchange(
        uri,
        HttpMethod.GET,
        new HttpEntity<>(headers),
        UnsplashSearchResponse.class
    ).getBody();

    if (response == null) {
      return Optional.empty();
    }
    return response.resultsOrEmpty().stream()
        .filter(this::hasRequiredFields)
        .findFirst()
        .map(this::toImage);
  }

  private boolean hasRequiredFields(UnsplashPhoto photo) {
    return photo.urls() != null && photo.urls().raw() != null && !photo.urls().raw().isBlank()
        && photo.user() != null && photo.user().name() != null
        && photo.user().links() != null && photo.user().links().html() != null;
  }

  private UnsplashImage toImage(UnsplashPhoto photo) {
    // Unsplash 가이드라인: 작가와 Unsplash 링크에는 utm_source/utm_medium을 붙인다.
    String authorPageUrl = UriComponentsBuilder.fromUriString(photo.user().links().html())
        .queryParam("utm_source", properties.appName())
        .queryParam("utm_medium", "referral")
        .build()
        .toUriString();
    return new UnsplashImage(photo.urls().raw(), photo.user().name(), authorPageUrl);
  }
}
