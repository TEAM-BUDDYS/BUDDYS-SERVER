package org.sopt.buddys.domain.location.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.location.entity.City;

public record CityResponse(
    @Schema(description = "도시 ID", example = "1") Long id,
    @Schema(description = "도시 이름(영문)", example = "Seoul") String name,
    @Schema(description = "도시 이름(한글)", example = "서울", nullable = true) String koreanName
) {
  public static CityResponse from(City city) {
    return new CityResponse(city.getId(), city.getName(), city.getKoreanName());
  }
}
