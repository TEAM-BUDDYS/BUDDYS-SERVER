package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.location.dto.response.CountryResponse;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.user.service.result.UserCountriesResult;

public record UserCountriesResponse(
    @Schema(description = "관심 국가. 설정하지 않은 경우 null입니다.", nullable = true)
    CountryResponse interestCountry,
    @Schema(description = "파견 국가. 설정하지 않은 경우 null입니다.", nullable = true)
    CountryResponse exchangeCountry
) {

  public static UserCountriesResponse from(UserCountriesResult result) {
    return new UserCountriesResponse(
        toCountryResponse(result.interestCountry()),
        toCountryResponse(result.exchangeCountry())
    );
  }

  private static CountryResponse toCountryResponse(Country country) {
    return country == null ? null : CountryResponse.from(country);
  }
}
