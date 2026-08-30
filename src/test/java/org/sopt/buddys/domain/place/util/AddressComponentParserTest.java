package org.sopt.buddys.domain.place.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.place.client.dto.GoogleAddressComponent;

class AddressComponentParserTest {

  @DisplayName("country 타입에서 국가명을, locality 타입에서 도시명을 뽑는다")
  @Test
  void extractsCountryAndCity() {
    List<GoogleAddressComponent> components = List.of(
        new GoogleAddressComponent("루브르", "루브르", List.of("point_of_interest", "establishment")),
        new GoogleAddressComponent("파리", "파리", List.of("locality", "political")),
        new GoogleAddressComponent("일드프랑스", "IDF", List.of("administrative_area_level_1", "political")),
        new GoogleAddressComponent("프랑스", "FR", List.of("country", "political"))
    );

    assertThat(AddressComponentParser.extractCountry(components)).isEqualTo("프랑스");
    assertThat(AddressComponentParser.extractCity(components)).isEqualTo("파리");
  }

  @DisplayName("locality가 없으면 상위 행정구역으로 도시를 대체한다")
  @Test
  void fallsBackToAdministrativeAreaWhenNoLocality() {
    List<GoogleAddressComponent> components = List.of(
        new GoogleAddressComponent("캘리포니아", "CA", List.of("administrative_area_level_1", "political")),
        new GoogleAddressComponent("미국", "US", List.of("country", "political"))
    );

    assertThat(AddressComponentParser.extractCity(components)).isEqualTo("캘리포니아");
  }

  @DisplayName("components가 null이거나 해당 타입이 없으면 null을 반환한다")
  @Test
  void returnsNullWhenAbsent() {
    assertThat(AddressComponentParser.extractCountry(null)).isNull();
    assertThat(AddressComponentParser.extractCity(null)).isNull();
    assertThat(AddressComponentParser.extractCity(List.of(
        new GoogleAddressComponent("프랑스", "FR", List.of("country", "political"))
    ))).isNull();
  }
}
