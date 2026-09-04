package org.sopt.buddys.domain.course.service.command;

import java.time.LocalDate;
import java.util.List;

public record CreateCourseCommand(
    List<Long> countryIds,
    List<Long> cityIds,
    String title,
    String content,
    String thumbnailImageUrl,
    LocalDate startDate,
    LocalDate endDate,
    List<Long> tagIds,
    List<Long> companionUserIds,
    List<CourseDayCommand> days,
    List<CourseFlightCommand> flights
) {
}
