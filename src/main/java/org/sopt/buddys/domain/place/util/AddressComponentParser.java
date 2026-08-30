package org.sopt.buddys.domain.place.util;

import java.util.List;
import org.sopt.buddys.domain.place.client.dto.GoogleAddressComponent;

/**
 * 구글 Places {@code addressComponents}에서 국가/도시 표시값을 뽑는다.
 * 반환값은 {@code languageCode=ko} 기준의 구글 표기 문자열이며, 우리 country/city 마스터 데이터와 연결되지 않는다.
 */
public final class AddressComponentParser {

  private static final String COUNTRY_TYPE = "country";

  // 도시: locality(시)를 우선하고, 안 내려오는 국가를 위해 상위 행정구역/생활권으로 폴백한다.
  private static final List<String> CITY_TYPES_IN_PRIORITY = List.of(
      "locality",
      "postal_town",
      "administrative_area_level_1",
      "sublocality_level_1"
  );

  private AddressComponentParser() {
  }

  public static String extractCountry(List<GoogleAddressComponent> components) {
    return firstLongText(components, List.of(COUNTRY_TYPE));
  }

  public static String extractCity(List<GoogleAddressComponent> components) {
    for (String type : CITY_TYPES_IN_PRIORITY) {
      String value = firstLongText(components, List.of(type));
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private static String firstLongText(List<GoogleAddressComponent> components, List<String> wantedTypes) {
    if (components == null) {
      return null;
    }
    return components.stream()
        .filter(component -> wantedTypes.stream().anyMatch(component::hasType))
        .map(GoogleAddressComponent::longText)
        .filter(text -> text != null && !text.isBlank())
        .findFirst()
        .orElse(null);
  }
}
