package org.sopt.buddys.domain.course.service.command;

public record CourseSearchCondition(
    Long countryId,
    Long tagId
) {
}
