package org.sopt.buddys.domain.course.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record CourseFlightRequest(
    @Schema(description = "항공사", example = "대한항공")
    @NotBlank
    @Size(max = 100)
    String airline,

    @Schema(description = "항공편명", example = "KE901")
    @Size(max = 20)
    String flightNumber,

    @Schema(description = "출발 공항", example = "ICN")
    @NotBlank
    @Size(max = 100)
    String departureAirport,

    @Schema(description = "출발 시간 (HH:mm)", type = "string", example = "13:00")
    @NotNull
    @JsonFormat(pattern = "HH:mm")
    LocalTime departureTime,

    @Schema(description = "도착 공항", example = "CDG")
    @NotBlank
    @Size(max = 100)
    String arrivalAirport,

    @Schema(description = "도착 시간 (HH:mm)", type = "string", example = "18:30")
    @NotNull
    @JsonFormat(pattern = "HH:mm")
    LocalTime arrivalTime
) {
}
