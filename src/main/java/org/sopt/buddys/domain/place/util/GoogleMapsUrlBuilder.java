package org.sopt.buddys.domain.place.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 구글맵 웹/앱으로 이동하는 딥링크(Google Maps URLs, api=1)를 만든다.
 * <a href="https://developers.google.com/maps/documentation/urls/get-started#search-action">Search action</a>은
 * {@code query}가 필수이며, {@code query_place_id}가 있어도 place_id를 처리하지 못하는 클라이언트를 위해
 * {@code query}에는 장소명/주소 같은 사람이 읽을 값을 함께 담아야 한다.
 */
public final class GoogleMapsUrlBuilder {

  private static final String SEARCH_URL_TEMPLATE =
      "https://www.google.com/maps/search/?api=1&query=%s&query_place_id=%s";

  private GoogleMapsUrlBuilder() {
  }

  public static String toPlaceUrl(String googlePlaceId, String query) {
    String safeQuery = (query == null || query.isBlank()) ? googlePlaceId : query;
    return SEARCH_URL_TEMPLATE.formatted(
        URLEncoder.encode(safeQuery, StandardCharsets.UTF_8),
        URLEncoder.encode(googlePlaceId, StandardCharsets.UTF_8)
    );
  }
}
