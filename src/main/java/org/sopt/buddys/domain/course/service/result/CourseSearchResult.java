package org.sopt.buddys.domain.course.service.result;

public record CourseSearchResult(
    CourseListResult courses,
    long totalElements
) {
}
