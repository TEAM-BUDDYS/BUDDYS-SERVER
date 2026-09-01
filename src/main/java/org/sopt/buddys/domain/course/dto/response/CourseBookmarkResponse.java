package org.sopt.buddys.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CourseBookmarkResponse(
    @Schema(description = "코스 ID", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    Long courseId,

    @Schema(description = "저장(찜) 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean bookmarked
) {
}
