package org.sopt.buddys.domain.location.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.location.entity.Country;

public record CountryResponse(
    @Schema(description = "국가 ID", example = "1") Long id,
    @Schema(description = "국가 이름", example = "대한민국") String name,
    @Schema(description = "국가 코드", example = "KR") String code
) {
  public static CountryResponse from(Country country) {
    return new CountryResponse(country.getId(), country.getName(), country.getIsoCode());
  }
}