package org.sopt.buddys.domain.post.service.result;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.sopt.buddys.domain.post.entity.AgeCondition;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.GenderCondition;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.sopt.buddys.domain.post.entity.RecruitmentCountType;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.Gender;

public record PostDetailResult(
    Long postId,
    AuthorResult author,
    boolean isMine,
    PostStatus recruitmentStatus,
    String title,
    List<String> imageUrls,
    CountryResult country,
    CityResult city,
    LocalDate startDate,
    LocalDate endDate,
    RecruitmentCountType recruitmentCountType,
    String content,
    List<AgeCondition> ageConditions,
    GenderCondition genderCondition,
    CompanionType travelType,
    List<TagResult> tags,
    Long viewCount,
    Long commentCount,
    LocalDateTime createdAt
) {

  public PostDetailResult {
    imageUrls = List.copyOf(imageUrls);
    ageConditions = List.copyOf(ageConditions);
    tags = List.copyOf(tags);
  }

  public record AuthorResult(
      Long userId,
      String nickname,
      String profileImageUrl,
      String country,
      Integer age,
      String ageRange,
      Gender gender
  ) {
  }

  public record CityResult(
      Long cityId,
      String name
  ) {
  }

  public record CountryResult(
      Long countryId,
      String name
  ) {
  }

  public record TagResult(
      Long tagId,
      String name,
      TagType tagType
  ) {
  }
}
