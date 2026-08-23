package org.sopt.buddys.domain.course.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CourseDayRequest(
    @Schema(description = "일차 (1부터 시작)", example = "1")
    @NotNull
    @Positive
    Short dayNumber,

    @Schema(description = "해당 일자의 실제 날짜", example = "2026-09-01")
    LocalDate date,

    @Schema(description = "해당 일자의 사진 목록 (최대 10장)", example = "[\"https://example.com/a.jpg\"]")
    @Size(max = 10)
    List<@NotBlank @Size(max = 512) String> imageUrls,

    @Schema(description = "해당 일자에 방문한 장소 목록")
    List<@Valid CoursePlaceRequest> places
) {
}
