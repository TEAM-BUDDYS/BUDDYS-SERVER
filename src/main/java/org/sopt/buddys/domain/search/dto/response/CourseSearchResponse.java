package org.sopt.buddys.domain.search.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.course.dto.response.CourseListResponse;
import org.sopt.buddys.domain.course.dto.response.CourseListResponse.CourseSummaryResponse;
import org.sopt.buddys.domain.course.service.result.CourseSearchResult;

public record CourseSearchResponse(
    @Schema(description = "코스 검색 결과", requiredMode = Schema.RequiredMode.REQUIRED)
    List<CourseSummaryResponse> content,

    @Schema(description = "현재 페이지 번호", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    int page,

    @Schema(description = "페이지 크기", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    int size,

    @Schema(description = "검색 조건에 일치하는 전체 코스 수. page=0에서만 제공하며 page>0에서는 null입니다.",
        example = "12", nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonInclude(JsonInclude.Include.ALWAYS)
    Long totalElements,

    @Schema(description = "다음 페이지 존재 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean hasNext
) {

  public CourseSearchResponse {
    content = List.copyOf(content);
  }

  public static CourseSearchResponse from(CourseSearchResult result) {
    CourseListResponse courses = CourseListResponse.from(result.courses());
    return new CourseSearchResponse(
        courses.content(), courses.page(), courses.size(), result.totalElements(), courses.hasNext()
    );
  }
}
