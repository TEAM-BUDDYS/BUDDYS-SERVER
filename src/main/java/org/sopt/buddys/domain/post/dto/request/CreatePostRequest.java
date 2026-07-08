package org.sopt.buddys.domain.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.GenderCondition;

public record CreatePostRequest(
    @Schema(description = "국가 ID", example = "1")
    @NotNull
    Long countryId,

    @Schema(description = "도시 ID", example = "10")
    Long cityId,

    @Schema(description = "동행 시작일", example = "2026-09-06")
    LocalDate startDate,

    @Schema(description = "동행 종료일", example = "2026-09-19")
    LocalDate endDate,

    @Schema(description = "게시글 제목", example = "주말에 파리 근교 함께 가실 분!")
    @NotBlank
    @Size(max = 120)
    String title,

    @Schema(description = "게시글 본문", example = "안녕하세요. 파리 근교 여행 동행을 구합니다.")
    @NotBlank
    String content,

    @Schema(description = "희망 연령대")
    @Valid
    AgeRange ageRange,

    @Schema(description = "성별 조건", example = "ANY", allowableValues = {"ANY", "MALE", "FEMALE"})
    @NotNull
    GenderCondition gender,

    @Schema(
        description = "동행 유형. FULL_TRIP(여행 전체 동행), PARTIAL_TRIP(여행 부분 동행), "
            + "ACCOMMODATION_SHARE(숙박 공유), TOUR(투어 동행), MEAL(식사 동행), "
            + "DAILY_LIFE(생활 동행), GROUP_PURCHASE(공동 구매)",
        example = "FULL_TRIP"
    )
    @NotNull
    CompanionType companionType,

    @Schema(description = "최대 모집 인원", example = "4")
    @NotNull
    @Min(1)
    Short maxParticipants,

    @Schema(description = "연결할 태그 ID 목록", example = "[1, 2, 3]")
    List<@NotNull Long> tagIds,

    @Schema(description = "이미 업로드된 게시글 이미지 URL 목록", example = "[\"https://example.com/post-image.png\"]")
    List<@NotBlank @Size(max = 512) String> imageUrls
) {

  public record AgeRange(
      @Schema(description = "최소 나이", example = "20")
      @Min(0)
      Short minAge,

      @Schema(description = "최대 나이", example = "29")
      @Min(0)
      Short maxAge
  ) {
  }
}
