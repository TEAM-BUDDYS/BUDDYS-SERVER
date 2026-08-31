package org.sopt.buddys.domain.course.service.command;

import java.time.LocalDateTime;

public record CourseFlightCommand(
    String airline,
    String flightNumber,
    String departureAirport,
    LocalDateTime departureAt,
    String arrivalAirport,
    LocalDateTime arrivalAt
) {
}
