package org.sopt.buddys.domain.course.service.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CourseDayCommand(
    Short dayNumber,
    LocalDate date,
    List<String> imageUrls,
    String memo,
    BigDecimal cost,
    List<CoursePlaceCommand> places,
    List<CourseFlightCommand> flights
) {
}
