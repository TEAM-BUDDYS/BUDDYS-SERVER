package org.sopt.buddys.domain.location.dto.response;

import java.util.List;
import org.sopt.buddys.domain.location.entity.City;
import org.springframework.data.domain.Slice;

public record CityListResponse(
    List<CityResponse> cities,
    int page,
    int size,
    boolean hasNext
) {
  public static CityListResponse from(Slice<City> slice) {
    return new CityListResponse(
        slice.getContent().stream().map(CityResponse::from).toList(),
        slice.getNumber(),
        slice.getSize(),
        slice.hasNext()
    );
  }

}
