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
    @Schema(description = "국가 ID", example = "3")
    @NotNull
    Long countryId,

    @Schema(description = "도시 ID", example = "21")
    @NotNull
    Long cityId,

    @Schema(description = "코스 제목", example = "파리 5일 코스")
    @NotBlank
    @Size(max = 120)
    String title,

    @Schema(description = "코스 소개", example = "루브르부터...")
    String content,

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
