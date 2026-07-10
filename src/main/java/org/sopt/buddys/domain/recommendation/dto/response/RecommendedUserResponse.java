package org.sopt.buddys.domain.recommendation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.Period;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.recommendation.service.result.RecommendedUserResult;
import org.sopt.buddys.domain.user.entity.User;

public record RecommendedUserResponse(
    @Schema(description = "사용자 ID", example = "1")
    Long userId,

    @Schema(description = "사용자 닉네임", example = "가윤")
    String nickname,

    @Schema(description = "파견 국가")
    CountryResponse exchangeCountry,

    @Schema(description = "연령대", example = "20대")
    String ageRange,

    @Schema(description = "매칭 퍼센트", example = "87")
    int matchingPercentage,

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.png")
    String profileImageUrl
) {

  public static RecommendedUserResponse from(RecommendedUserResult result) {
    User user = result.user();
    return new RecommendedUserResponse(
        user.getId(),
        user.getNickname(),
        CountryResponse.from(user.getExchangeCountry()),
        toAgeRange(user.getBirthDate()),
        (int) Math.round(result.totalSimilarity() * 100),
        user.getProfileImageUrl()
    );
  }

  private static String toAgeRange(LocalDate birthDate) {
    if (birthDate == null) {
      return null;
    }
    int age = Period.between(birthDate, LocalDate.now()).getYears();
    if (age < 10) {
      return "10대 미만";
    }
    return "%d0대".formatted(age / 10);
  }

  public record CountryResponse(
      @Schema(description = "국가 ID", example = "1")
      Long countryId,

      @Schema(description = "국가 이름", example = "France")
      String name
  ) {

    private static CountryResponse from(Country country) {
      if (country == null) {
        return null;
      }
      return new CountryResponse(country.getId(), country.getName());
    }
  }
}
