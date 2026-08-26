package org.sopt.buddys.domain.course.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.sopt.buddys.domain.course.service.command.CourseSearchCondition;

public record CourseListRequest(
    @Schema(description = "국가 ID", example = "240")
    Long countryId,

    @Schema(description = "페이지 번호. 0 이상입니다.", example = "0")
    @Min(0)
    Integer page,

    @Schema(description = "페이지 크기. 1 이상 100 이하입니다.", example = "20")
    @Max(100)
    @Min(1)
    Integer size
) {

  public int pageOrDefault() {
    return page == null ? 0 : page;
  }

  public int sizeOrDefault() {
    return size == null ? 20 : size;
  }

  public CourseSearchCondition toCondition() {
    return new CourseSearchCondition(countryId);
  }
}
