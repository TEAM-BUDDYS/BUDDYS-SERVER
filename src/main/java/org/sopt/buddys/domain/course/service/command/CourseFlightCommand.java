package org.sopt.buddys.domain.course.service.command;

import java.time.LocalTime;

public record CourseFlightCommand(
    String airline,
    String flightNumber,
    String departureAirport,
    LocalTime departureTime,
    String arrivalAirport,
    LocalTime arrivalTime
) {
}
