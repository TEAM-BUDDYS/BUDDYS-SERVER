package org.sopt.buddys.domain.airline.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.airline.entity.Airline;

public record AirlineResponse(
    @Schema(description = "항공사 ID", example = "1") Long id,
    @Schema(description = "항공사명", example = "대한항공") String name,
    @Schema(description = "항공사 코드 (IATA)", example = "KE") String code
) {

  public static AirlineResponse from(Airline airline) {
    return new AirlineResponse(airline.getId(), airline.getName(), airline.getCode());
  }
}
