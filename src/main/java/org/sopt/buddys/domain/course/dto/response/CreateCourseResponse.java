package org.sopt.buddys.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.course.entity.Course;

public record CreateCourseResponse(
    @Schema(description = "생성된 코스 ID", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    Long courseId
) {

  public static CreateCourseResponse from(Course course) {
    return new CreateCourseResponse(course.getId());
  }
}
