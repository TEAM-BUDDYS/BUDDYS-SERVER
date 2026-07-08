package org.sopt.buddys.domain.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.sopt.buddys.domain.post.entity.AgeCondition;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.GenderCondition;
import org.sopt.buddys.domain.post.entity.RecruitmentCountType;

public record CreatePostRequest(
    @Schema(description = "국가 ID", example = "1")
    @NotNull
    Long countryId,

    @Schema(description = "도시 ID", example = "10")
    @NotNull
    Long cityId,

    @Schema(description = "동행 시작일", example = "2026-09-06")
    @NotNull
    @FutureOrPresent
    LocalDate startDate,

    @Schema(description = "동행 종료일", example = "2026-09-19")
    @NotNull
    @FutureOrPresent
    LocalDate endDate,

    @Schema(description = "게시글 제목", example = "주말에 파리 근교 함께 가실 분!")
    @NotBlank
    @Size(max = 120)
    String title,

    @Schema(description = "게시글 본문", example = "안녕하세요. 파리 근교 여행 동행을 구합니다.")
    @NotBlank
    String content,

    @Schema(
        description = "희망 나이 조건. EARLY_20S(20대 초반), MID_20S(20대 중반), "
            + "LATE_20S(20대 후반), OVER_30S(30대 이상). 중복 선택 가능",
        example = "[\"EARLY_20S\", \"MID_20S\"]"
    )
    @NotEmpty
    List<@NotNull AgeCondition> ageConditions,

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

    @Schema(
        description = "모집 인원. UNDECIDED(미정), ONE(1인), TWO(2인), THREE(3인), FOUR_OR_MORE(4인 이상)",
        example = "TWO"
    )
    @NotNull
    RecruitmentCountType recruitmentCountType,

    @Schema(description = "연결할 태그 ID 목록", example = "[1, 2, 3]")
    @NotEmpty
    List<@NotNull Long> tagIds,

    @Schema(description = "이미 업로드된 게시글 이미지 URL 목록", example = "[\"https://example.com/post-image.png\"]")
    @Size(max = 10)
    List<@NotBlank @Size(max = 512) String> imageUrls
) {
}
