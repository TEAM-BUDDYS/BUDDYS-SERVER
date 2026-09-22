package org.sopt.buddys.domain.airline.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.airline.entity.Airline;

public record AirlineResponse(
    @Schema(description = "항공사 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long id,

    @Schema(description = "항공사명(영문)", example = "Korean Air", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,

    @Schema(
        description = "항공사명(국문). 없으면 null입니다.",
        example = "대한항공",
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true
    )
    String koreanName,

    @Schema(description = "항공사 코드 (IATA)", example = "KE", requiredMode = Schema.RequiredMode.REQUIRED)
    String code
) {

  public static AirlineResponse from(Airline airline) {
    return new AirlineResponse(airline.getId(), airline.getName(), airline.getKoreanName(), airline.getCode());
  }
}
