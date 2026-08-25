package org.sopt.buddys.domain.course.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateCourseRequest(
    @Schema(description = "국가 ID", example = "240")
    @NotNull
    Long countryId,

    @Schema(description = "도시 ID", example = "11160")
    @NotNull
    Long cityId,

    @Schema(description = "코스 제목", example = "여유로운 파리 미술관 코스")
    @NotBlank
    @Size(max = 120)
    String title,

    @Schema(description = "코스 소개", example = "2박 3일 코스로 다녀왔다. 버디즈로 구한 동행 친구와 함께했다.")
    String content,

    @Schema(description = "대표 사진 URL (날짜에 종속되지 않는 코스 전체 대표 이미지 1장)", example = "https://example.com/thumbnail.jpg")
    @Size(max = 512)
    String thumbnailImageUrl,

    @Schema(description = "출발일", example = "2026-09-01")
    @NotNull
    LocalDate startDate,

    @Schema(description = "도착일", example = "2026-09-05")
    @NotNull
    LocalDate endDate,

    @Schema(description = "연결할 태그 ID 목록 (활동 최대 3개, 관심사 최대 2개, 동행스타일 최대 2개, 활동 태그 1개 이상 필수)", example = "[1, 4, 9]")
    @NotEmpty
    @Size(max = 10)
    List<@NotNull Long> tagIds,

    @Schema(description = "함께한 유저 ID 목록 (최대 20명)", example = "[12, 30]")
    @Size(max = 20)
    List<@NotNull Long> companionUserIds,

    @Schema(description = "일자별 코스 목록 (최대 30일)")
    @NotEmpty
    @Size(max = 30)
    List<@Valid CourseDayRequest> days,

    @Schema(description = "항공편 목록 (최대 10개)")
    @Size(max = 10)
    List<@Valid CourseFlightRequest> flights
) {
}
