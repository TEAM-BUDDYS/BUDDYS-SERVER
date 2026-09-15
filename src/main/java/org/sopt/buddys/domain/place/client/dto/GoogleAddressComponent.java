package org.sopt.buddys.domain.place.client.dto;

import java.util.List;

/**
 * 구글 Places 주소 구성요소. {@code types}에 {@code "country"}, {@code "locality"},
 * {@code "administrative_area_level_1"} 등이 담겨 국가/도시를 식별한다.
 */
public record GoogleAddressComponent(
    String longText,
    String shortText,
    List<String> types
) {
  public boolean hasType(String type) {
    return types != null && types.contains(type);
  }
}
