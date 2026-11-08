package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.course.service.result.SecondSectionCountry;
import org.sopt.buddys.domain.course.service.result.SecondSectionCountryReason;
import org.sopt.buddys.domain.location.dto.response.CountryResponse;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.user.service.result.UserCountriesResult;

public record UserCountriesResponse(
    @Schema(description = "관심 국가. 설정하지 않은 경우 null입니다.", nullable = true)
    CountryResponse interestCountry,
    @Schema(description = "파견 국가. 설정하지 않은 경우 null입니다.", nullable = true)
    CountryResponse exchangeCountry,
    @Schema(description = "홈 두 번째 코스 섹션 국가. 파견 국가가 있고 관심 국가와 다르면 파견 국가, "
        + "아니면 관심 국가를 제외하고 코스가 가장 많은 국가입니다. 보여줄 국가가 없으면 null입니다.", nullable = true)
    SecondSectionCountryResponse secondSectionCountry
) {

  public static UserCountriesResponse from(UserCountriesResult result) {
    return new UserCountriesResponse(
        toCountryResponse(result.interestCountry()),
        toCountryResponse(result.exchangeCountry()),
        SecondSectionCountryResponse.from(result.secondSectionCountry())
    );
  }

  private static CountryResponse toCountryResponse(Country country) {
    return country == null ? null : CountryResponse.from(country);
  }

  public record SecondSectionCountryResponse(
      @Schema(description = "국가 ID", example = "31") Long id,
      @Schema(description = "국가 이름", example = "프랑스") String name,
      @Schema(description = "영문 국가 이름. 영문명이 없는 국가는 null입니다.", example = "France", nullable = true)
      String englishName,
      @Schema(description = "국가 코드", example = "FR") String code,
      @Schema(description = "선정 이유. EXCHANGE(파견 국가), MOST_COURSES(관심 국가를 제외하고 코스가 가장 많은 국가)",
          example = "EXCHANGE")
      SecondSectionCountryReason reason
  ) {

    private static SecondSectionCountryResponse from(SecondSectionCountry secondSectionCountry) {
      if (secondSectionCountry == null) {
        return null;
      }
      Country country = secondSectionCountry.country();
      return new SecondSectionCountryResponse(
          country.getId(),
          country.getName(),
          country.getEnglishName(),
          country.getIsoCode(),
          secondSectionCountry.reason()
      );
    }
  }
}
