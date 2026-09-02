package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.user.service.result.UserCoursesResult;

public record UserCoursesResponse(
    @Schema(
        description = "사용자가 작성한 코스 목록",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<CourseResponse> courses,

    @Schema(
        description = "현재 페이지 번호. 0부터 시작합니다.",
        example = "0",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    int page,

    @Schema(
        description = "페이지 크기",
        example = "18",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    int size,

    @Schema(
        description = "다음 페이지 존재 여부",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean hasNext
) {

  public UserCoursesResponse {
    courses = List.copyOf(courses);
  }

  public static UserCoursesResponse from(UserCoursesResult result) {
    return new UserCoursesResponse(
        result.courses()
            .stream()
            .map(CourseResponse::from)
            .toList(),
        result.page(),
        result.size(),
        result.hasNext()
    );
  }

  public record CourseResponse(
      @Schema(
          description = "코스 ID",
          example = "1",
          requiredMode = Schema.RequiredMode.REQUIRED
      )
      Long courseId,

      @Schema(
          description = "프로필에 표시할 코스 썸네일 URL (가장 이른 일차의 첫 사진)",
          example = "https://example.com/course-thumbnail.png",
          requiredMode = Schema.RequiredMode.REQUIRED,
          nullable = true
      )
      String thumbnailImageUrl
  ) {

    private static CourseResponse from(UserCoursesResult.CourseResult result) {
      return new CourseResponse(result.courseId(), result.thumbnailImageUrl());
    }
  }
}
