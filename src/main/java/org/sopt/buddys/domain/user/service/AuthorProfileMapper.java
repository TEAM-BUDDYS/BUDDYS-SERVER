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
    boolean withdrawn = author.getDeletedAt() != null;
    Country exchangeCountry = author.getExchangeCountry();
    return new AuthorProfile(
        author.getId(),
        withdrawn ? WITHDRAWN_USER_NICKNAME : author.getNickname(),
        withdrawn ? null : author.getProfileImageUrl(),
        exchangeCountry == null ? null : exchangeCountry.getName(),
        toAge(author.getBirthDate()),
        toAgeRange(author.getBirthDate()),
        author.getGender()
    );
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
