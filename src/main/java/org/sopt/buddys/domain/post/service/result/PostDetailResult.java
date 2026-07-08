package org.sopt.buddys.domain.post.service.result;

import java.time.LocalDate;
import java.util.List;
import org.sopt.buddys.domain.post.entity.AgeCondition;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.sopt.buddys.domain.post.entity.RecruitmentCountType;
import org.sopt.buddys.domain.user.entity.Gender;

public record PostDetailResult(
    Long postId,
    AuthorResult author,
    PostStatus recruitmentStatus,
    String title,
    List<String> imageUrls,
    CityResult city,
    LocalDate startDate,
    LocalDate endDate,
    RecruitmentCountType recruitmentCountType,
    String content,
    List<AgeCondition> ageConditions,
    CompanionType travelType,
    List<TagResult> tags,
    Long viewCount,
    Long commentCount
) {

  public PostDetailResult {
    imageUrls = List.copyOf(imageUrls);
    ageConditions = List.copyOf(ageConditions);
    tags = List.copyOf(tags);
  }

  public record AuthorResult(
      String name,
      String country,
      String ageRange,
      Gender gender
  ) {
  }

  public record CityResult(
      Long cityId,
      String name
  ) {
  }

  public record TagResult(
      Long tagId,
      String name
  ) {
  }
}
