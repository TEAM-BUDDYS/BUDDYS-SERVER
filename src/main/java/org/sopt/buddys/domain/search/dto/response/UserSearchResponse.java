package org.sopt.buddys.domain.search.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.search.service.result.UserSearchResult;

public record UserSearchResponse(
    @Schema(description = "사용자 검색 결과", requiredMode = Schema.RequiredMode.REQUIRED)
    List<UserSummaryResponse> content,

    @Schema(description = "현재 페이지 번호", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    int page,

    @Schema(description = "페이지 크기", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    int size,

    @Schema(description = "다음 페이지 존재 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean hasNext
) {

  public UserSearchResponse {
    content = List.copyOf(content);
  }

  public static UserSearchResponse from(UserSearchResult result) {
    return new UserSearchResponse(
        result.content().stream().map(UserSummaryResponse::from).toList(),
        result.page(),
        result.size(),
        result.hasNext()
    );
  }

  public record UserSummaryResponse(
      @Schema(description = "사용자 ID", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
      Long userId,

      @Schema(description = "닉네임", example = "파리여행자", requiredMode = Schema.RequiredMode.REQUIRED)
      String nickname,

      @Schema(description = "프로필 이미지 URL", nullable = true)
      String profileImageUrl
  ) {

    private static UserSummaryResponse from(UserSearchResult.UserSummaryResult result) {
      return new UserSummaryResponse(result.userId(), result.nickname(), result.profileImageUrl());
    }
  }
}
