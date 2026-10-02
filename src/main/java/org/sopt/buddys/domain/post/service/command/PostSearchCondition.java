package org.sopt.buddys.domain.post.service.command;

import java.time.LocalDate;
import java.util.List;
import org.sopt.buddys.domain.post.entity.AgeCondition;
import org.sopt.buddys.domain.post.entity.AuthorVerification;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.GenderCondition;

public record PostSearchCondition(
    String keyword,
    Long countryId,
    LocalDate startDate,
    LocalDate endDate,
    List<AgeCondition> ageConditions,
    List<GenderCondition> genderConditions,
    List<CompanionType> companionTypes,
    Long tagId,
    List<AuthorVerification> authorVerifications
) {

  public static PostSearchCondition keywordOnly(String keyword) {
    return new PostSearchCondition(keyword, null, null, null, null, null, null, null, null);
  }
}
