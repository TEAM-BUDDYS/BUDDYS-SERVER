package org.sopt.buddys.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.course.service.result.CourseListResult;

public record CourseListResponse(
    @Schema(description = "코스 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    List<CourseSummaryResponse> content,

    @Schema(description = "현재 페이지 번호. 0부터 시작합니다.", example = "0",
        requiredMode = Schema.RequiredMode.REQUIRED)
    int page,

    @Schema(description = "페이지 크기", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
    int size,

    @Schema(description = "다음 페이지 존재 여부", example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED)
    boolean hasNext
) {

  public CourseListResponse {
    content = List.copyOf(content);
  }

  public static CourseListResponse from(CourseListResult result) {
    return new CourseListResponse(
        result.content().stream()
            .map(CourseSummaryResponse::from)
            .toList(),
        result.page(),
        result.size(),
        result.hasNext()
    );
  }

  public record CourseSummaryResponse(
      @Schema(description = "코스 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
      Long courseId,

      @Schema(description = "코스 제목", example = "여유로운 파리 미술관 코스",
          requiredMode = Schema.RequiredMode.REQUIRED)
      String title,

      @Schema(description = "코스 소개", example = "2박 3일 코스로 다녀왔다. 버디즈로 구한 동행 친구와 함께했다.",
          requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
      String content,

      @Schema(description = "로그인 사용자의 코스 저장 여부", example = "false",
          requiredMode = Schema.RequiredMode.REQUIRED)
      boolean isBookmarked,

      @Schema(description = "일자별 사진을 합친 이미지 목록",
          example = "[\"https://example.com/day1.jpg\"]",
          requiredMode = Schema.RequiredMode.REQUIRED)
      List<String> images,

      @Schema(description = "여행 국가 목록 (쉼표로 구분)", example = "체코, 독일",
          requiredMode = Schema.RequiredMode.REQUIRED)
      String countries,

      @Schema(description = "여행 도시 목록 (쉼표로 구분)", example = "프라하, 뮌헨, 베를린",
          requiredMode = Schema.RequiredMode.REQUIRED)
      String cities
  ) {

    public CourseSummaryResponse {
      images = List.copyOf(images);
    }

    private static CourseSummaryResponse from(CourseListResult.CourseSummaryResult result) {
      return new CourseSummaryResponse(
          result.courseId(),
          result.title(),
          result.content(),
          result.isBookmarked(),
          result.images(),
          result.countries(),
          result.cities()
      );
    }
  }
}
