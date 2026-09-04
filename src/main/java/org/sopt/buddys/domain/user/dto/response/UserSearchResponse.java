package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.user.entity.User;
import org.springframework.data.domain.Slice;

public record UserSearchResponse(
    @Schema(description = "검색된 사용자 목록. keyword가 없으면 항상 빈 리스트입니다.")
    List<UserSearchResultResponse> users,

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    int page,

    @Schema(description = "페이지 크기", example = "20")
    int size,

    @Schema(description = "다음 페이지 존재 여부", example = "false")
    boolean hasNext
) {

  public UserSearchResponse {
    users = List.copyOf(users);
  }

  public static UserSearchResponse from(Slice<User> slice) {
    return new UserSearchResponse(
        slice.getContent().stream().map(UserSearchResultResponse::from).toList(),
        slice.getNumber(),
        slice.getSize(),
        slice.hasNext()
    );
  }
}
