package org.sopt.buddys.domain.user.service;

import java.time.LocalDate;
import java.time.Period;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.service.result.AuthorProfile;

public final class AuthorProfileMapper {

  public static final String WITHDRAWN_USER_NICKNAME = "탈퇴한 사용자";

  private AuthorProfileMapper() {
  }

  public static AuthorProfile toAuthorProfile(User author) {
    Country exchangeCountry = author.getExchangeCountry();
    return new AuthorProfile(
        author.getId(),
        maskedNickname(author),
        maskedProfileImageUrl(author),
        exchangeCountry == null ? null : exchangeCountry.getName(),
        toAge(author.getBirthDate()),
        toAgeRange(author.getBirthDate()),
        author.getGender()
    );
  }

  public static String maskedNickname(User user) {
    return user.getDeletedAt() != null ? WITHDRAWN_USER_NICKNAME : user.getNickname();
  }

  public static String maskedProfileImageUrl(User user) {
    return user.getDeletedAt() != null ? null : user.getProfileImageUrl();
  }

  private static String toAgeRange(LocalDate birthDate) {
    Integer age = toAge(birthDate);
    if (age == null) {
      return null;
    }
    if (age < 10) {
      return "10대 미만";
    }
    return "%d0대".formatted(age / 10);
  }

  private static Integer toAge(LocalDate birthDate) {
    if (birthDate == null) {
      return null;
    }
    return Period.between(birthDate, LocalDate.now()).getYears();
  }
}
