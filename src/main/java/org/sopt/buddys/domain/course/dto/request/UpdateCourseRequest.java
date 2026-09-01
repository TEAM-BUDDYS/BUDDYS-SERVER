package org.sopt.buddys.domain.course.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record UpdateCourseRequest(
    @Schema(description = "국가 ID 목록", example = "[240]")
    @NotEmpty
    List<@NotNull Long> countryIds,

    @Schema(description = "도시 ID 목록", example = "[11160]")
    @NotEmpty
    List<@NotNull Long> cityIds,

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

    @Schema(description = "일자별 코스 목록 (최대 30일). 기존 일자/장소/사진 정보를 모두 대체합니다.")
    @NotEmpty
    @Size(max = 30)
    List<@NotNull @Valid CourseDayRequest> days,

    @Schema(description = "항공편 목록 (최대 5개). 기존 항공편 정보를 모두 대체합니다.")
    @Size(max = 5)
    List<@NotNull @Valid CourseFlightRequest> flights
) {
}
