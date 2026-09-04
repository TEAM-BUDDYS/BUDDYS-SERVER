package org.sopt.buddys.domain.user.service.result;

import java.util.List;

public record UserCoursesResult(
    List<CourseResult> courses,
    int page,
    int size,
    boolean hasNext
) {

  public UserCoursesResult {
    courses = List.copyOf(courses);
  }

  public record CourseResult(
      Long courseId,
      String thumbnailImageUrl
  ) {
  }
}
