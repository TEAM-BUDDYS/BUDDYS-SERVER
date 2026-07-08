package org.sopt.buddys.domain.location.dto.response;

import java.util.List;
import org.sopt.buddys.domain.location.entity.Country;
import org.springframework.data.domain.Slice;

public record CountryListResponse(
    List<CountryResponse> countries,
    int page,
    int size,
    boolean hasNext
) {
  public static CountryListResponse from(Slice<Country> slice) {
    return new CountryListResponse(
        slice.getContent().stream().map(CountryResponse::from).toList(),
        slice.getNumber(),
        slice.getSize(),
        slice.hasNext()
    );
  }

}
