package org.sopt.buddys.domain.course.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CoursePlaceRequest(
    @Schema(description = "구글맵 place_id", example = "ChIJ...")
    @NotBlank
    @Size(max = 512)
    String googlePlaceId,

    @Schema(description = "장소명", example = "루브르 박물관")
    @NotBlank
    @Size(max = 255)
    String name,

    @Schema(description = "장소 카테고리. RESTAURANT, CAFE, TOURISM, ACCOMMODATION, ETC", example = "TOURISM")
    @NotBlank
    String category,

    @Schema(description = "위도", example = "48.8606")
    BigDecimal latitude,

    @Schema(description = "경도", example = "2.3376")
    BigDecimal longitude,

    @Schema(description = "하루 내 방문 순서", example = "0")
    @PositiveOrZero
    Short orderNo,

    @Schema(description = "주소. 장소 검색 응답의 address를 그대로 전달", example = "Rue de Rivoli, 75001 Paris", nullable = true)
    @Size(max = 512)
    String address,

    @Schema(description = "국가명. 장소 검색 응답의 country를 그대로 전달", example = "프랑스", nullable = true)
    @Size(max = 100)
    String country,

    @Schema(description = "도시명. 장소 검색 응답의 city를 그대로 전달", example = "파리", nullable = true)
    @Size(max = 100)
    String city
) {
}
