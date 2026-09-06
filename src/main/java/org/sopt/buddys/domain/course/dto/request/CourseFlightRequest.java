package org.sopt.buddys.domain.course.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

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

    @Schema(description = "출발일시", example = "2026-09-01T13:00:00")
    @NotNull
    LocalDateTime departureAt,

    @Schema(description = "도착 공항", example = "CDG")
    @NotBlank
    @Size(max = 100)
    String arrivalAirport,

    @Schema(description = "도착일시", example = "2026-09-01T18:30:00")
    @NotNull
    LocalDateTime arrivalAt
) {
}
