package org.sopt.buddys.domain.course.service.result;

import java.util.List;

public record CourseListResult(
    List<CourseSummaryResult> content,
    int page,
    int size,
    boolean hasNext
) {

  public CourseListResult {
    content = List.copyOf(content);
  }

  public record CourseSummaryResult(
      Long courseId,
      String title,
      String content,
      boolean isBookmarked,
      List<String> images,
      String countries,
      String cities
  ) {

    public CourseSummaryResult {
      images = List.copyOf(images);
    }
  }
}
