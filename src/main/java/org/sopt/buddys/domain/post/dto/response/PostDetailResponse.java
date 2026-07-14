package org.sopt.buddys.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.sopt.buddys.domain.post.entity.AgeCondition;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.GenderCondition;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.sopt.buddys.domain.post.entity.RecruitmentCountType;
import org.sopt.buddys.domain.post.service.result.PostDetailResult;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.Gender;

public record PostDetailResponse(
    @Schema(description = "게시글 ID", example = "1")
    Long postId,

    @Schema(description = "작성자 정보")
    AuthorResponse author,

    @Schema(description = "로그인 사용자의 게시글 여부", example = "false")
    boolean isMine,

    @Schema(description = "모집 상태", example = "RECRUITING")
    PostStatus recruitmentStatus,

    @Schema(description = "게시글 제목", example = "도쿄 같이 여행할 동행 구해요")
    String title,

    @Schema(description = "첨부 이미지 URL 목록")
    List<String> imageUrls,

    @Schema(description = "동행 모집 국가")
    CountryResponse country,

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
    Long commentCount,

    @Schema(description = "게시글 생성일시", example = "2027-02-20T14:30:00")
    LocalDateTime createdAt
) {

  public PostDetailResponse {
    imageUrls = List.copyOf(imageUrls);
  }

  public static PostDetailResponse from(PostDetailResult result) {
    return new PostDetailResponse(
        result.postId(),
        AuthorResponse.from(result.author()),
        result.isMine(),
        result.recruitmentStatus(),
        result.title(),
        result.imageUrls(),
        CountryResponse.from(result.country()),
        CityResponse.from(result.city()),
        result.startDate(),
        result.endDate(),
        result.recruitmentCountType(),
        result.content(),
        ConditionsResponse.from(result),
        result.viewCount(),
        result.commentCount(),
        result.createdAt()
    );
  }

  public record AuthorResponse(
      @Schema(description = "작성자 ID", example = "10")
      Long userId,

      @Schema(description = "작성자 닉네임", example = "김가윤")
      String nickname,

      @Schema(description = "작성자 프로필 이미지 URL", example = "https://example.com/profile.png")
      String profileImageUrl,

      @Schema(description = "작성자 국가", example = "대한민국")
      String country,

      @Schema(description = "작성자 나이", example = "24")
      Integer age,

      @Schema(description = "작성자 나이대", example = "20대")
      String ageRange,

      @Schema(description = "작성자 성별", example = "FEMALE")
      Gender gender
  ) {

    private static AuthorResponse from(PostDetailResult.AuthorResult author) {
      return new AuthorResponse(
          author.userId(),
          author.nickname(),
          author.profileImageUrl(),
          author.country(),
          author.age(),
          author.ageRange(),
          author.gender()
      );
    }
  }

  public record CityResponse(
      @Schema(description = "도시 ID", example = "1")
      Long cityId,

      @Schema(description = "도시 이름", example = "Tokyo")
      String name,

      @Schema(description = "도시 한글 이름", example = "도쿄")
      String koreanName
  ) {

    private static CityResponse from(PostDetailResult.CityResult city) {
      return new CityResponse(city.cityId(), city.name(), city.koreanName());
    }
  }

  public record CountryResponse(
      @Schema(description = "국가 ID", example = "1")
      Long countryId,

      @Schema(description = "국가 이름", example = "France")
      String name
  ) {

    private static CountryResponse from(PostDetailResult.CountryResult country) {
      return new CountryResponse(country.countryId(), country.name());
    }
  }

  public record ConditionsResponse(
      @Schema(description = "선호 나이 조건")
      List<AgeCondition> ageConditions,

      @Schema(description = "성별 조건", example = "[\"MALE\", \"FEMALE\"]")
      List<GenderCondition> genderConditions,

      @Schema(description = "동행 유형", example = "MEAL")
      CompanionType travelType,

      @Schema(description = "활동 태그 목록")
      List<TagResponse> activityTags,

      @Schema(description = "관심사 태그 목록")
      List<TagResponse> interestTags,

      @Schema(description = "여행 스타일 태그 목록")
      List<TagResponse> travelStyleTags
  ) {

    public ConditionsResponse {
      ageConditions = List.copyOf(ageConditions);
      genderConditions = List.copyOf(genderConditions);
      activityTags = List.copyOf(activityTags);
      interestTags = List.copyOf(interestTags);
      travelStyleTags = List.copyOf(travelStyleTags);
    }

    private static ConditionsResponse from(PostDetailResult result) {
      return new ConditionsResponse(
          result.ageConditions(),
          result.genderConditions(),
          result.travelType(),
          toTagResponses(result, TagType.ACTIVITY),
          toTagResponses(result, TagType.INTEREST),
          toTagResponses(result, TagType.TRAVEL_STYLE)
      );
    }

    private static List<TagResponse> toTagResponses(PostDetailResult result, TagType tagType) {
      return result.tags()
          .stream()
          .filter(tag -> tag.tagType() == tagType)
          .map(TagResponse::from)
          .toList();
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
