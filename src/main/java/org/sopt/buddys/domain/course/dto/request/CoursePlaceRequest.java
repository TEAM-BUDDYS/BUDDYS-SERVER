package org.sopt.buddys.domain.course.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
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

    @Schema(description = "메모", example = "예약 필수")
    @Size(max = 500)
    String memo,

    @Schema(description = "비용", example = "22000")
    @DecimalMin(value = "0", inclusive = true)
    BigDecimal cost
) {
}
