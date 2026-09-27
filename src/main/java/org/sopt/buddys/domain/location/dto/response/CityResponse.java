package org.sopt.buddys.domain.location.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import org.sopt.buddys.domain.location.entity.City;
import org.sopt.buddys.domain.location.service.CityRecommendedRadiusCalculator;

public record CityResponse(
    @Schema(description = "도시 ID", example = "1") Long id,
    @Schema(description = "도시 이름(영문)", example = "Seoul") String name,
    @Schema(description = "도시 이름(한글)", example = "서울", nullable = true) String koreanName,

    @Schema(description = "도시 중심 위도. 근처 장소 조회(/api/v1/places/nearby) 요청 시 lat로 사용할 수 있습니다.",
        example = "37.5665", nullable = true)
    BigDecimal latitude,

    @Schema(description = "도시 중심 경도. 근처 장소 조회(/api/v1/places/nearby) 요청 시 lng로 사용할 수 있습니다.",
        example = "126.978", nullable = true)
    BigDecimal longitude,

    @Schema(description = "도시 인구 기반으로 추정한 추천 검색 반경(미터). "
        + "근처 장소 조회(/api/v1/places/nearby) 요청 시 radius로 사용할 수 있습니다.", example = "25000")
    int recommendedRadius
) {
  public static CityResponse from(City city) {
    return new CityResponse(
        city.getId(),
        city.getName(),
        city.getKoreanName(),
        city.getLatitude(),
        city.getLongitude(),
        CityRecommendedRadiusCalculator.calculate(city.getPopulation())
    );
  }
}
