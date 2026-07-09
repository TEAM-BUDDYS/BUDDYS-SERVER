package org.sopt.buddys.domain.comment.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.SuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentSuccessCode implements SuccessCode {

  COMMENT_CREATED("COMMENT-S001", HttpStatus.CREATED, "댓글 작성에 성공했습니다."),
  COMMENT_LIST_FOUND("COMMENT-S002", HttpStatus.OK, "댓글 목록 조회에 성공했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
