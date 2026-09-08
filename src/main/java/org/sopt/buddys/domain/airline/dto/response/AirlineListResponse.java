package org.sopt.buddys.domain.airline.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.airline.entity.Airline;
import org.springframework.data.domain.Slice;

public record AirlineListResponse(
    @Schema(description = "검색된 항공사 목록. keyword가 없으면 항상 빈 리스트입니다.") List<AirlineResponse> airlines,
    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0") int page,
    @Schema(description = "페이지 크기", example = "20") int size,
    @Schema(description = "다음 페이지 존재 여부", example = "false") boolean hasNext
) {

  public AirlineListResponse {
    airlines = List.copyOf(airlines);
  }

  public static AirlineListResponse from(Slice<Airline> slice) {
    return new AirlineListResponse(
        slice.getContent().stream().map(AirlineResponse::from).toList(),
        slice.getNumber(),
        slice.getSize(),
        slice.hasNext()
    );
  }
}
