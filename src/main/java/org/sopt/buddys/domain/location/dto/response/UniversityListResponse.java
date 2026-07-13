package org.sopt.buddys.domain.location.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.location.entity.University;
import org.springframework.data.domain.Slice;

public record UniversityListResponse(
    @Schema(description = "검색된 대학교 목록. keyword가 없으면 항상 빈 리스트입니다.") List<UniversityResponse> universities,
    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0") int page,
    @Schema(description = "페이지 크기", example = "20") int size,
    @Schema(description = "다음 페이지 존재 여부", example = "false") boolean hasNext
) {
  public static UniversityListResponse from(Slice<University> slice) {
    return new UniversityListResponse(
        slice.getContent().stream().map(UniversityResponse::from).toList(),
        slice.getNumber(),
        slice.getSize(),
        slice.hasNext()
    );
  }
}