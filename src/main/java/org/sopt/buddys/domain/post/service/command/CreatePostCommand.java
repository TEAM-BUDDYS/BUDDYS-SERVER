package org.sopt.buddys.domain.post.service.command;

import java.time.LocalDate;
import java.util.List;
import org.sopt.buddys.domain.post.entity.AgeCondition;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.GenderCondition;
import org.sopt.buddys.domain.post.entity.RecruitmentCountType;

public record CreatePostCommand(
    Long countryId,
    Long cityId,
    LocalDate startDate,
    LocalDate endDate,
    String title,
    String content,
    List<AgeCondition> ageConditions,
    GenderCondition gender,
    CompanionType companionType,
    RecruitmentCountType recruitmentCountType,
    List<Long> tagIds,
    List<String> imageUrls
) {
}
