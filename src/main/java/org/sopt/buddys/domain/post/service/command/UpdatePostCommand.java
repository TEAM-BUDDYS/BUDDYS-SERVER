package org.sopt.buddys.domain.post.service.command;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.sopt.buddys.domain.post.dto.request.UpdatePostRequest.Field;
import org.sopt.buddys.domain.post.entity.AgeCondition;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.GenderCondition;
import org.sopt.buddys.domain.post.entity.RecruitmentCountType;

public record UpdatePostCommand(
    Long countryId, Long cityId, LocalDate startDate, LocalDate endDate,
    String title, String content, List<AgeCondition> ageConditions,
    List<GenderCondition> genderConditions, CompanionType companionType,
    RecruitmentCountType recruitmentCountType, List<Long> tagIds,
    List<String> imageUrls, Set<Field> providedFields
) {
  public boolean isProvided(Field field) {
    return providedFields.contains(field);
  }

  public boolean isEmpty() {
    return providedFields.isEmpty();
  }
}
