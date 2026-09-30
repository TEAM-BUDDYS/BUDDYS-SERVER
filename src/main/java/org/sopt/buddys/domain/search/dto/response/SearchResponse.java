package org.sopt.buddys.domain.search.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.post.dto.response.PostListResponse;
import org.sopt.buddys.domain.search.service.result.SearchResult;

public record SearchResponse(
    @Schema(description = "코스 검색 결과", requiredMode = Schema.RequiredMode.REQUIRED)
    CourseSearchResponse courses,

    @Schema(description = "사용자 검색 결과", requiredMode = Schema.RequiredMode.REQUIRED)
    UserSearchResponse users,

    @Schema(description = "동행 게시글 검색 결과", requiredMode = Schema.RequiredMode.REQUIRED)
    PostListResponse posts
) {

  public static SearchResponse from(SearchResult result) {
    return new SearchResponse(
        CourseSearchResponse.from(result.courses()),
        UserSearchResponse.from(result.users()),
        PostListResponse.from(result.posts())
    );
  }
}
