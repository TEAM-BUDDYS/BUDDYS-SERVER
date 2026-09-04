package org.sopt.buddys.domain.post.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.SuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostSuccessCode implements SuccessCode {

  POST_DETAIL_FOUND("POST-S001", HttpStatus.OK, "게시글 상세 조회에 성공했습니다."),
  POST_STATUS_UPDATED("POST-S002", HttpStatus.OK, "모집 상태 변경에 성공했습니다."),
  POST_LIST_FOUND("POST-S003", HttpStatus.OK, "동행 게시글 목록 조회에 성공했습니다."),
  POST_UPDATED("POST-S004", HttpStatus.OK, "게시글 수정에 성공했습니다."),
  POST_DELETED("POST-S005", HttpStatus.OK, "게시글 삭제에 성공했습니다."),
  POST_BOOKMARKED("POST-S006", HttpStatus.OK, "게시글 저장에 성공했습니다."),
  POST_BOOKMARK_REMOVED("POST-S007", HttpStatus.OK, "게시글 저장 취소에 성공했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
