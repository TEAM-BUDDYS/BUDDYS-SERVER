package org.sopt.buddys.domain.location.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.location.entity.University;

public record UniversityResponse(
    @Schema(description = "대학교 ID", example = "1") Long id,
    @Schema(description = "대학교 이름", example = "Yonsei University") String name,
    @Schema(description = "대학교 도메인", example = "yonsei.ac.kr", nullable = true) String domain
) {
  public static UniversityResponse from(University university) {
    return new UniversityResponse(university.getId(), university.getName(), university.getDomain());
  }
}