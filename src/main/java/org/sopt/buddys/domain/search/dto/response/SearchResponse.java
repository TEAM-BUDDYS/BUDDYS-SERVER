package org.sopt.buddys.domain.search.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.post.dto.response.PostListResponse;
import org.sopt.buddys.domain.search.service.result.SearchResult;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchResponse(
    @Schema(description = "코스 검색 결과. type 생략 또는 COURSE 지정 시에만 포함", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    CourseSearchResponse courses,

    @Schema(description = "사용자 검색 결과. type 생략 또는 USER 지정 시에만 포함", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    UserSearchResponse users,

    @Schema(description = "동행 게시글 검색 결과. type 생략 또는 POST 지정 시에만 포함", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    PostListResponse posts
) {

  public static SearchResponse from(SearchResult result) {
    return new SearchResponse(
        result.courses() == null ? null : CourseSearchResponse.from(result.courses()),
        result.users() == null ? null : UserSearchResponse.from(result.users()),
        result.posts() == null ? null : PostListResponse.from(result.posts())
    );
  }
}
