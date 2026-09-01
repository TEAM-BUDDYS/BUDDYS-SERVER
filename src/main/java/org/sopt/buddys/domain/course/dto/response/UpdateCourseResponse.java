package org.sopt.buddys.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.course.entity.Course;

public record UpdateCourseResponse(
    @Schema(description = "수정된 코스 ID", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    Long courseId
) {

  public static UpdateCourseResponse from(Course course) {
    return new UpdateCourseResponse(course.getId());
  }
}
