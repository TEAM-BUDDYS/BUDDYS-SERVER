package org.sopt.buddys.domain.magazine.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record MagazineListRequest(

    @Schema(
        description = "조회 연도. month와 함께 전달하거나 둘 다 생략해야 합니다. "
            + "둘 다 생략하면 Asia/Seoul 기준 현재 연도가 적용됩니다.",
        example = "2026"
    )
    @Min(1000)
    @Max(9999)
    Integer year,

    @Schema(
        description = "조회 월(1~12). year와 함께 전달하거나 둘 다 생략해야 합니다. "
            + "둘 다 생략하면 Asia/Seoul 기준 현재 월이 적용됩니다.",
        example = "8"
    )
    @Min(1)
    @Max(12)
    Integer month,

    @Schema(description = "페이지 번호. 0 이상입니다.", example = "0")
    @Min(0)
    Integer page,

    @Schema(description = "페이지 크기. 1 이상 100 이하입니다.", example = "10")
    @Min(1)
    @Max(100)
    Integer size
) {

  public int pageOrDefault() {
    return page == null ? 0 : page;
  }

  public int sizeOrDefault() {
    return size == null ? 10 : size;
  }
}
