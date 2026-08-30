package org.sopt.buddys.domain.place.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GoogleMapsUrlBuilderTest {

  @DisplayName("query와 query_place_id를 URL 인코딩해 구글맵 Search 딥링크를 만든다")
  @Test
  void toPlaceUrl_encodesQueryAndPlaceId() {
    String url = GoogleMapsUrlBuilder.toPlaceUrl("ChIJN1t_tDeuEmsRUsoyG83frY4", "루브르 박물관");

    assertThat(url).isEqualTo(
        "https://www.google.com/maps/search/?api=1"
            + "&query=%EB%A3%A8%EB%B8%8C%EB%A5%B4+%EB%B0%95%EB%AC%BC%EA%B4%80"
            + "&query_place_id=ChIJN1t_tDeuEmsRUsoyG83frY4");
  }

  @DisplayName("query가 없으면 place_id를 query로 대신 사용한다")
  @Test
  void toPlaceUrl_blankQuery_fallsBackToPlaceId() {
    String url = GoogleMapsUrlBuilder.toPlaceUrl("place-123", "  ");

    assertThat(url).isEqualTo(
        "https://www.google.com/maps/search/?api=1&query=place-123&query_place_id=place-123");
  }
}
