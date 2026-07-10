package org.sopt.buddys.domain.post.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.SuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostSuccessCode implements SuccessCode {

  POST_DETAIL_FOUND("POST-S001", HttpStatus.OK, "게시글 상세 조회에 성공했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
