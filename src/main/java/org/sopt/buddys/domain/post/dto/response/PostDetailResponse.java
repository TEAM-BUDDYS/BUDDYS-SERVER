package org.sopt.buddys.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import org.sopt.buddys.domain.location.entity.City;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.post.entity.AgeCondition;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.sopt.buddys.domain.post.entity.RecruitmentCountType;
import org.sopt.buddys.domain.post.service.result.PostDetailResult;
import org.sopt.buddys.domain.user.entity.Gender;
import org.sopt.buddys.domain.user.entity.User;

public record PostDetailResponse(
    @Schema(description = "게시글 ID", example = "1")
    Long postId,

    @Schema(description = "작성자 정보")
    AuthorResponse author,

    @Schema(description = "모집 상태", example = "RECRUITING")
    PostStatus recruitmentStatus,

    @Schema(description = "게시글 제목", example = "도쿄 같이 여행할 동행 구해요")
    String title,

    @Schema(description = "첨부 이미지 URL 목록")
    List<String> imageUrls,

    @Schema(description = "동행 모집 도시")
    CityResponse city,

    @Schema(description = "동행 일정 시작일", example = "2026-07-20")
    LocalDate startDate,

    @Schema(description = "동행 일정 종료일", example = "2026-07-22")
    LocalDate endDate,

    @Schema(description = "모집 인원 타입", example = "TWO")
    RecruitmentCountType recruitmentCountType,

    @Schema(description = "게시글 본문", example = "같이 맛집이랑 관광지 다니실 분 구해요.")
    String content,

    @Schema(description = "동행 조건")
    ConditionsResponse conditions,

    @Schema(description = "조회수", example = "11")
    Long viewCount,

    @Schema(description = "댓글 수", example = "3")
    Long commentCount
) {

  public PostDetailResponse {
    imageUrls = List.copyOf(imageUrls);
  }

  public static PostDetailResponse from(PostDetailResult result) {
    Post post = result.post();
    return new PostDetailResponse(
        post.getId(),
        AuthorResponse.from(post.getAuthor()),
        post.getStatus(),
        post.getTitle(),
        result.imageUrls(),
        CityResponse.from(post.getCity()),
        post.getStartDate(),
        post.getEndDate(),
        post.getRecruitmentCountType(),
        post.getContent(),
        ConditionsResponse.from(post, result),
        post.getViewCount(),
        post.getCommentCount()
    );
  }

  public record AuthorResponse(
      @Schema(description = "작성자 이름", example = "김가윤")
      String name,

      @Schema(description = "작성자 국가", example = "대한민국")
      String country,

      @Schema(description = "작성자 나이대", example = "20대")
      String ageRange,

      @Schema(description = "작성자 성별", example = "FEMALE")
      Gender gender
  ) {

    private static AuthorResponse from(User author) {
      Country country = author.getExchangeCountry();
      return new AuthorResponse(
          author.getNickname(),
          country == null ? null : country.getName(),
          toAgeRange(author.getBirthDate()),
          author.getGender()
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
  }

  public record CityResponse(
      @Schema(description = "도시 ID", example = "1")
      Long cityId,

      @Schema(description = "도시 이름", example = "Tokyo")
      String name
  ) {

    private static CityResponse from(City city) {
      return new CityResponse(city.getId(), city.getName());
    }
  }

  public record ConditionsResponse(
      @Schema(description = "선호 나이 조건")
      List<AgeCondition> ageConditions,

      @Schema(description = "동행 유형", example = "MEAL")
      CompanionType travelType,

      @Schema(description = "취향 태그 목록")
      List<TagResponse> tags
  ) {

    public ConditionsResponse {
      ageConditions = List.copyOf(ageConditions);
      tags = List.copyOf(tags);
    }

    private static ConditionsResponse from(Post post, PostDetailResult result) {
      return new ConditionsResponse(
          result.ageConditions(),
          post.getCompanionType(),
          result.tags()
              .stream()
              .map(TagResponse::from)
              .toList()
      );
    }
  }

  public record TagResponse(
      @Schema(description = "태그 ID", example = "1")
      Long tagId,

      @Schema(description = "태그 이름", example = "맛집")
      String name
  ) {

    private static TagResponse from(PostDetailResult.TagResult tag) {
      return new TagResponse(tag.tagId(), tag.name());
    }
  }
}
