package org.sopt.buddys.domain.course.service.command;

import java.time.LocalDate;
import java.util.List;

public record CourseDayCommand(
    Short dayNumber,
    LocalDate date,
    List<String> imageUrls,
    List<CoursePlaceCommand> places
) {
}
